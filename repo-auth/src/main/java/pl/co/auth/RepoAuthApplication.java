package pl.co.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"pl.co.auth", "pl.co.common"})
public class RepoAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepoAuthApplication.class, args);
    }
}

