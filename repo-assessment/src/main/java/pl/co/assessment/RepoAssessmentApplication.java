package pl.co.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"pl.co.assessment", "pl.co.common"})
public class RepoAssessmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepoAssessmentApplication.class, args);
    }
}
