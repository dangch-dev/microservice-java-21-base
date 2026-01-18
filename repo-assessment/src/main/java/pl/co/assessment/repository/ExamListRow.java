package pl.co.assessment.repository;

public interface ExamListRow {
    String getExamId();
    String getExamVersionId();
    String getCategoryName();
    String getName();
    String getStatus();
    Integer getDurationMinutes();
    Boolean getShuffleQuestions();
    Boolean getShuffleOptions();
}
