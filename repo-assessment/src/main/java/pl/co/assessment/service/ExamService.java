package pl.co.assessment.service;

import pl.co.assessment.dto.ExamCreateRequest;
import pl.co.assessment.dto.ExamCreateResponse;
import pl.co.assessment.dto.ExamDraftSaveRequest;
import pl.co.assessment.dto.ExamEditorResponse;
import pl.co.assessment.dto.ExamPageResponse;

public interface ExamService {
    ExamCreateResponse create(ExamCreateRequest request);

    ExamPageResponse list(String searchValue, String categoryId, Integer page, Integer size);

    ExamEditorResponse requestEdit(String examId);

    void saveDraft(String examId, ExamDraftSaveRequest request);

    void discardDraft(String examId);

    void publishDraft(String examId);
}
