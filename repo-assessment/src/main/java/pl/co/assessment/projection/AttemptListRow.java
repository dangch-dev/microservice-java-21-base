package pl.co.assessment.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface AttemptListRow {
    String getAttemptId();
    String getExamId();
    String getExamVersionId();
    String getName();
    String getDescription();
    Integer getDurationMinutes();
    String getStatus();
    String getGradingStatus();
    Instant getStartTime();
    Instant getEndTime();
    BigDecimal getScore();
    BigDecimal getMaxScore();
    BigDecimal getPercent();
}
