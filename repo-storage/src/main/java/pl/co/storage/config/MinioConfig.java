package pl.co.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        StorageProperties.MinioProperties minio = properties.getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Bean
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient, StorageProperties properties) {
        return args -> {
            StorageProperties.MinioProperties minio = properties.getMinio();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minio.getBucket()).build());
            if (!exists) {
                MakeBucketArgs.Builder builder = MakeBucketArgs.builder().bucket(minio.getBucket());
                if (minio.getRegion() != null && !minio.getRegion().isBlank()) {
                    builder.region(minio.getRegion());
                }
                log.info("Creating MinIO bucket {}", minio.getBucket());
                minioClient.makeBucket(builder.build());
            }
        };
    }
}
