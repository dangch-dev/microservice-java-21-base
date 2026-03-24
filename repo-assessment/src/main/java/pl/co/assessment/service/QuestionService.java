package pl.co.assessment.service;

import pl.co.assessment.dto.ExamDraftChangeRequest;

public interface QuestionService {

    String normalizeQuestionType(String type, String questionId);

    void processQuestionPayload(ExamDraftChangeRequest change, String normalizedType);
}
