package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.ExamEditorMetadata;
import pl.co.assessment.dto.ExamEditorQuestion;
import pl.co.assessment.dto.ExamEditorResponse;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.repository.ExamEditorQuestionRow;
import pl.co.assessment.repository.ExamRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.service.ExamVersionService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamVersionServiceImpl implements ExamVersionService {

    private final ExamRepository examRepository;
    private final ExamVersionRepository examVersionRepository;

    @Override
    @Transactional(readOnly = true)
    public ExamEditorResponse previewVersion(String examVersionId) {
        ExamVersion version = examVersionRepository.findByIdAndDeletedFalse(examVersionId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227,
                        ErrorCode.E227.message("Exam version not found")));

        ExamEditorMetadata metadata = new ExamEditorMetadata(
                version.getName(),
                version.getDescription(),
                version.getDurationMinutes(),
                version.isShuffleQuestions(),
                version.isShuffleOptions()
        );

        List<ExamEditorQuestionRow> rows = examRepository.findEditorQuestionsByVersionId(version.getId());
        List<ExamEditorQuestion> questions = new ArrayList<>();
        for (ExamEditorQuestionRow row : rows) {
            questions.add(new ExamEditorQuestion(
                    row.getQuestionId(),
                    row.getQuestionOrder(),
                    row.getQuestionVersionId(),
                    row.getType(),
                    row.getQuestionContent(),
                    row.getGradingRules()
            ));
        }

        return new ExamEditorResponse(
                version.getStatus(),
                version.getId(),
                metadata,
                questions
        );
    }
}
