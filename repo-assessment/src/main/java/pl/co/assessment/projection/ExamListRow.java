package pl.co.assessment.projection;

public interface ExamListRow {
    String getExamId();
    String getExamVersionId();
    String getCategoryName();
    String getName();
    String getDescription();
    String getStatus();
    Integer getDurationMinutes();
    Boolean getShuffleQuestions();
    Boolean getShuffleOptions();
    Boolean getEnabled();
}
