package pl.co.realtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"pl.co.realtime", "pl.co.common"})
public class RepoRealtimeApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepoRealtimeApplication.class, args);
    }
}
