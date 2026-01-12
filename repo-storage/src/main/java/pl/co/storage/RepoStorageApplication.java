package pl.co.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"pl.co.storage", "pl.co.common"})
public class RepoStorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepoStorageApplication.class, args);
    }
}
