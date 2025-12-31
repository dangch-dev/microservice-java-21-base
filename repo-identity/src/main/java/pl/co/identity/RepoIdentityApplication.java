package pl.co.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"pl.co.identity", "pl.co.common"})
public class RepoIdentityApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepoIdentityApplication.class, args);
    }
}
