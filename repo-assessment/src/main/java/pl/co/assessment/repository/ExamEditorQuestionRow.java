package pl.co.assessment.repository;

import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;

public interface ExamEditorQuestionRow {
    String getQuestionId();
    Integer getQuestionOrder();
    String getQuestionVersionId();
    String getType();
    QuestionContent getQuestionContent();
    GradingRules getGradingRules();
}
