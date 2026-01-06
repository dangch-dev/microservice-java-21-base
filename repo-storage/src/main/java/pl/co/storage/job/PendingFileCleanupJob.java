package pl.co.storage.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.co.storage.service.FileService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingFileCleanupJob {

    private final FileService fileService;

    @Scheduled(cron = "${storage.cleanup-cron:0 0 * * * *}")
    public void cleanup() {
        try {
            fileService.cleanupPending();
        } catch (Exception ex) {
            log.error("Pending file cleanup failed", ex);
        }
    }
}
