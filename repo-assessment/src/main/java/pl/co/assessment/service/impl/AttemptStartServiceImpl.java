package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.AttemptStartMode;
import pl.co.assessment.dto.AttemptStartResponse;
import pl.co.assessment.entity.AttemptOptionOrder;
import pl.co.assessment.entity.AttemptQuestionOrder;
import pl.co.assessment.entity.Exam;
import pl.co.assessment.entity.ExamAttempt;
import pl.co.assessment.entity.ExamAttemptGradingStatus;
import pl.co.assessment.entity.ExamAttemptStatus;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.ExamVersionStatus;
import pl.co.assessment.entity.QuestionType;
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.repository.AttemptOptionOrderRepository;
import pl.co.assessment.repository.AttemptQuestionOrderRepository;
import pl.co.assessment.repository.ExamAttemptRepository;
import pl.co.assessment.repository.ExamRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.service.AttemptStartService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AttemptStartServiceImpl implements AttemptStartService {

    private final ExamRepository examRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamVersionQuestionRepository examVersionQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AttemptQuestionOrderRepository attemptQuestionOrderRepository;
    private final AttemptOptionOrderRepository attemptOptionOrderRepository;
    private final QuestionVersionRepository questionVersionRepository;

    @Override
    @Transactional
    public AttemptStartResponse startAttempt(String examId, String userId) {
        Exam exam = loadExamOrThrow(examId);
        ExamVersion published = loadPublishedVersionOrThrow(exam);

        ExamAttempt activeAttempt = findActiveAttemptForUpdate(examId, userId);
        if (activeAttempt != null) {
            return buildResumeResponse(exam, activeAttempt);
        }
        return createOrResumeOnConflict(exam, published, examId, userId);
    }

    private Exam loadExamOrThrow(String examId) {
        // Validate exam existence + enabled flag
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Exam not found")));
        if (!exam.isEnabled()) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Exam is disabled"));
        }
        return exam;
    }

    private ExamVersion loadPublishedVersionOrThrow(Exam exam) {
        // Validate published pointer + published status
        String publishedId = exam.getPublishedExamVersionId();
        if (publishedId == null || publishedId.isBlank()) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Published exam version does not exist"));
        }
        ExamVersion published = examVersionRepository.findByIdAndExamIdAndDeletedFalse(publishedId, exam.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.E420, ErrorCode.E420.message("Published exam version does not exist")));
        if (!ExamVersionStatus.PUBLISHED.name().equalsIgnoreCase(published.getStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Exam version is not published"));
        }
        return published;
    }

    private ExamAttempt findActiveAttemptForUpdate(String examId, String userId) {
        // Lock active attempts to avoid double-start race
        List<String> statuses = List.of(
                ExamAttemptStatus.IN_PROGRESS.name(),
                ExamAttemptStatus.TIMEOUT.name());
        List<ExamAttempt> attempts = examAttemptRepository.findActiveAttemptsForUpdate(examId, userId, statuses);
        return attempts.isEmpty() ? null : attempts.getFirst();
    }

    private AttemptStartResponse buildResumeResponse(Exam exam, ExamAttempt attempt) {
        // Load attempt version to compute remaining time
        ExamVersion attemptVersion = examVersionRepository.findByIdAndExamIdAndDeletedFalse(attempt.getExamVersionId(), exam.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.E420, ErrorCode.E420.message("Exam version does not exist")));
        long remainingSeconds = computeRemainingSeconds(attempt, attemptVersion);
        return AttemptStartResponse.builder()
                .attemptId(attempt.getId())
                .mode(AttemptStartMode.RESUME)
                .examId(exam.getId())
                .examVersionId(attempt.getExamVersionId())
                .name(attemptVersion.getName())
                .description(attemptVersion.getDescription())
                .startTime(attempt.getStartTime())
                .durationMinutes(attemptVersion.getDurationMinutes())
                .timeRemainingSeconds(remainingSeconds)
                .build();
    }

    private long computeRemainingSeconds(ExamAttempt attempt, ExamVersion attemptVersion) {
        // SUBMITTED or TIMEOUT returns remaining = 0
        if (ExamAttemptStatus.SUBMITTED.name().equalsIgnoreCase(attempt.getStatus())
                || ExamAttemptStatus.TIMEOUT.name().equalsIgnoreCase(attempt.getStatus())) {
            return 0L;
        }
        long durationSeconds = attemptVersion.getDurationMinutes() == null ? 0L : attemptVersion.getDurationMinutes() * 60L;
        if (durationSeconds <= 0L || attempt.getStartTime() == null) {
            return 0L;
        }
        long elapsed = Duration.between(attempt.getStartTime(), Instant.now()).getSeconds();
        long remaining = durationSeconds - elapsed;
        return Math.max(0L, remaining);
    }

    private ExamAttempt createAttempt(Exam exam, ExamVersion published) {
        // Insert new attempt with IN_PROGRESS status + default AUTO_GRADING
        Instant now = Instant.now();
        ExamAttempt attempt = ExamAttempt.builder()
                .examId(exam.getId())
                .examVersionId(published.getId())
                .startTime(now)
                .status(ExamAttemptStatus.IN_PROGRESS.name())
                .gradingStatus(ExamAttemptGradingStatus.AUTO_GRADING.name())
                .build();
        return examAttemptRepository.save(attempt);
    }

    private void persistAttemptOrdersIfNeeded(String attemptId, ExamVersion published) {
        boolean shuffleQuestions = Boolean.TRUE.equals(published.isShuffleQuestions());
        boolean shuffleOptions = Boolean.TRUE.equals(published.isShuffleOptions());
        if (!shuffleQuestions && !shuffleOptions) {
            return;
        }
        List<ExamVersionQuestion> mappings =
                examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(published.getId());
        if (mappings.isEmpty()) {
            return;
        }
        if (shuffleQuestions) {
            persistQuestionOrder(attemptId, mappings);
        }
        if (shuffleOptions) {
            persistOptionOrder(attemptId, mappings);
        }
    }

    private void persistQuestionOrder(String attemptId, List<ExamVersionQuestion> mappings) {
        // Persist shuffled question order per attempt
        List<ExamVersionQuestion> shuffled = new ArrayList<>(mappings);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        List<AttemptQuestionOrder> orders = new ArrayList<>();
        int displayOrder = 1;
        for (ExamVersionQuestion item : shuffled) {
            orders.add(AttemptQuestionOrder.builder()
                    .attemptId(attemptId)
                    .examVersionQuestionId(item.getId())
                    .displayOrder(displayOrder++)
                    .build());
        }
        if (!orders.isEmpty()) {
            attemptQuestionOrderRepository.saveAll(orders);
        }
    }

    private void persistOptionOrder(String attemptId, List<ExamVersionQuestion> mappings) {
        // Persist shuffled option order for choice questions only
        List<String> questionVersionIds = mappings.stream()
                .map(ExamVersionQuestion::getQuestionVersionId)
                .distinct()
                .toList();
        if (questionVersionIds.isEmpty()) {
            return;
        }
        Map<String, QuestionVersion> questionVersionMap = loadQuestionVersions(questionVersionIds);
        List<AttemptOptionOrder> orders = new ArrayList<>();
        for (ExamVersionQuestion mapping : mappings) {
            QuestionVersion qv = questionVersionMap.get(mapping.getQuestionVersionId());
            if (qv == null) {
                continue;
            }
            if (!isChoiceType(qv.getType())) {
                continue;
            }
            QuestionContent content = qv.getQuestionContent();
            if (content == null || content.getOptions() == null || content.getOptions().isEmpty()) {
                continue;
            }
            List<String> optionKeys = content.getOptions().stream()
                    .map(QuestionContent.Option::getId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
            if (optionKeys.isEmpty()) {
                continue;
            }
            List<String> shuffled = new ArrayList<>(optionKeys);
            Collections.shuffle(shuffled, ThreadLocalRandom.current());
            int displayOrder = 1;
            for (String key : shuffled) {
                orders.add(AttemptOptionOrder.builder()
                        .attemptId(attemptId)
                        .questionVersionId(qv.getId())
                        .optionKey(key)
                        .displayOrder(displayOrder++)
                        .build());
            }
        }
        if (!orders.isEmpty()) {
            attemptOptionOrderRepository.saveAll(orders);
        }
    }

    private Map<String, QuestionVersion> loadQuestionVersions(List<String> questionVersionIds) {
        List<QuestionVersion> questionVersions = questionVersionRepository.findByIdInAndDeletedFalse(questionVersionIds);
        Map<String, QuestionVersion> questionVersionMap = new HashMap<>();
        for (QuestionVersion qv : questionVersions) {
            questionVersionMap.put(qv.getId(), qv);
        }
        return questionVersionMap;
    }

    private boolean isChoiceType(String type) {
        return QuestionType.SINGLE_CHOICE.name().equalsIgnoreCase(type)
                || QuestionType.MULTIPLE_CHOICE.name().equalsIgnoreCase(type);
    }

    private AttemptStartResponse buildNewResponse(Exam exam, ExamVersion published, ExamAttempt created) {
        // Build NEW response
        long durationSeconds = published.getDurationMinutes() == null ? 0L : published.getDurationMinutes() * 60L;
        return AttemptStartResponse.builder()
                .attemptId(created.getId())
                .mode(AttemptStartMode.NEW)
                .examId(exam.getId())
                .examVersionId(published.getId())
                .name(published.getName())
                .description(published.getDescription())
                .startTime(created.getStartTime())
                .durationMinutes(published.getDurationMinutes())
                .timeRemainingSeconds(durationSeconds)
                .build();
    }

    private AttemptStartResponse createOrResumeOnConflict(Exam exam,
                                                          ExamVersion published,
                                                          String examId,
                                                          String userId) {
        try {
            ExamAttempt created = createAttempt(exam, published);
            persistAttemptOrdersIfNeeded(created.getId(), published);
            return buildNewResponse(exam, published, created);
        } catch (DataIntegrityViolationException ex) {
            ExamAttempt resumed = findActiveAttemptForUpdate(examId, userId);
            if (resumed != null) {
                return buildResumeResponse(exam, resumed);
            }
            throw ex;
        }
    }
}
