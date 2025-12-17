package pl.co.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class RepoApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepoApiGatewayApplication.class, args);
    }
}
