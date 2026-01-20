package pl.co.assessment.service;

import pl.co.assessment.dto.ExamEditorResponse;

public interface ExamVersionService {
    ExamEditorResponse previewVersion(String examVersionId);
}
