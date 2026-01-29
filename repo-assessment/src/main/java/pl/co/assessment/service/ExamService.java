package pl.co.assessment.service;

import pl.co.assessment.dto.ExamCreateRequest;
import pl.co.assessment.dto.ExamCreateResponse;
import pl.co.assessment.dto.ExamDraftSaveRequest;
import pl.co.assessment.dto.ExamEditorResponse;
import pl.co.assessment.dto.ExamPageResponse;
import pl.co.assessment.dto.ExamStatusUpdateRequest;

public interface ExamService {
    ExamCreateResponse create(ExamCreateRequest request);

    ExamPageResponse list(String searchValue, String categoryId, Boolean enabled, Integer page, Integer size);

    ExamPageResponse listPublic(String searchValue, Integer page, Integer size);

    ExamEditorResponse requestEdit(String examId);

    void saveDraft(String examId, ExamDraftSaveRequest request);

    void discardDraft(String examId);

    void publishDraft(String examId);

    void updateStatus(String examId, ExamStatusUpdateRequest request);

    void deleteExam(String examId);
}
