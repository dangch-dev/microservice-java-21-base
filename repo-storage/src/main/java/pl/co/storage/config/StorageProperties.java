package pl.co.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@Validated
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private MinioProperties minio = new MinioProperties();

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration presignTtl = Duration.ofMinutes(15);

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration pendingTtl = Duration.ofHours(1);

    /**
     * Cron expression for pending cleanup scheduler. Default: every hour at minute 0.
     */
    @NotBlank
    private String cleanupCron = "0 0 * * * *";

    /**
     * When true, delete objects from MinIO on soft delete/cleanup.
     */
    private boolean deleteObjectOnSoftDelete = false;

    @Data
    public static class MinioProperties {
        @NotBlank
        private String endpoint;
        private String consoleEndpoint;
        private String region;
        @NotBlank
        private String accessKey;
        @NotBlank
        private String secretKey;
        @NotBlank
        private String bucket;
    }
}
