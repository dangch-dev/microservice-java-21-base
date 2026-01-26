package pl.co.assessment.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.co.assessment.service.impl.AttemptServiceImplImpl;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttemptTimeoutJob {

    private final AttemptServiceImplImpl attemptService;

    @Scheduled(cron = "${assessment.attempt-timeout-cron}")
    public void timeoutAttempts() {
        try {
            attemptService.applyTimeouts();
        } catch (Exception ex) {
            log.error("Attempt timeout job failed", ex);
        }
    }
}
