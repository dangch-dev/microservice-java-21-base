package pl.co.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"pl.co.notification", "pl.co.common"})
public class RepoNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepoNotificationApplication.class, args);
    }
}
