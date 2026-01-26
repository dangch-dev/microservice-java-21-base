package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.entity.ExamAttempt;
import pl.co.assessment.entity.ExamAttemptGradingStatus;
import pl.co.assessment.entity.ExamAttemptStatus;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.repository.ExamAttemptRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttemptServiceImplImpl implements pl.co.assessment.service.AttemptServiceImpl {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamVersionRepository examVersionRepository;

    @Override
    @Transactional
    public void submit(String attemptId, String userId) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndDeletedFalseForUpdate(attemptId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Attempt not found")));

        if (!userId.equals(attempt.getCreatedBy())) {
            throw new ApiException(ErrorCode.E230, ErrorCode.E230.message("No authority"));
        }

        if (ExamAttemptStatus.SUBMITTED.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt is already submitted"));
        }

        ExamVersion version = examVersionRepository.findByIdAndDeletedFalse(attempt.getExamVersionId())
                .orElseThrow(() -> new ApiException(ErrorCode.E420, ErrorCode.E420.message("Exam version does not exist")));

        if (attempt.getEndTime() == null) {
            Instant now = Instant.now();
            Instant deadline = computeDeadline(attempt.getStartTime(), version.getDurationMinutes());
            Instant endTime = (deadline != null && now.isAfter(deadline)) ? deadline : now;
            attempt.setEndTime(endTime);
        }

        if (attempt.getGradingStatus() == null) {
            attempt.setGradingStatus(ExamAttemptGradingStatus.AUTO_GRADING.name());
        }

        attempt.setStatus(ExamAttemptStatus.SUBMITTED.name());
        examAttemptRepository.save(attempt);

        // TODO: enqueue attemptId for grading
    }

    @Transactional
    public void applyTimeouts() {
        List<ExamAttempt> inProgress = examAttemptRepository
                .findByStatusAndDeletedFalse(ExamAttemptStatus.IN_PROGRESS.name());
        if (inProgress.isEmpty()) {
            return;
        }

        Map<String, ExamVersion> versions = loadVersions(inProgress);
        Instant now = Instant.now();
        boolean changed = false;
        for (ExamAttempt attempt : inProgress) {
            ExamVersion version = versions.get(attempt.getExamVersionId());
            if (version == null || attempt.getStartTime() == null || version.getDurationMinutes() == null) {
                continue;
            }
            Instant deadline = computeDeadline(attempt.getStartTime(), version.getDurationMinutes());
            if (deadline != null && now.isAfter(deadline)) {
                attempt.setStatus(ExamAttemptStatus.TIMEOUT.name());
                if (attempt.getEndTime() == null || attempt.getEndTime().isAfter(deadline)) {
                    attempt.setEndTime(deadline);
                }
                changed = true;
            }
        }

        if (changed) {
            examAttemptRepository.saveAll(inProgress);
        }
    }

    private Map<String, ExamVersion> loadVersions(List<ExamAttempt> attempts) {
        List<String> ids = attempts.stream()
                .map(ExamAttempt::getExamVersionId)
                .distinct()
                .toList();
        return examVersionRepository.findByIdInAndDeletedFalse(ids).stream()
                .collect(Collectors.toMap(ExamVersion::getId, version -> version));
    }

    private Instant computeDeadline(Instant startTime, Integer durationMinutes) {
        if (startTime == null || durationMinutes == null) {
            return null;
        }
        long durationSeconds = durationMinutes * 60L;
        if (durationSeconds <= 0L) {
            return startTime;
        }
        return startTime.plusSeconds(durationSeconds);
    }
}
