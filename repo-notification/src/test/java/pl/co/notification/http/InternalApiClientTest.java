package pl.co.notification.http;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import pl.co.common.http.InternalApiClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = InternalApiClient.class)
@TestPropertySource(properties = {
        "internal.api.base-pattern=http://%s",
        "internal.api.header=X-Internal-Token",
        "internal.api.token=test-secret"
})
class InternalApiClientTest {

    @MockBean
    RestTemplate restTemplate;

    @Autowired
    InternalApiClient client;

    @Test
    void send_shouldAttachInternalHeader_andCallLogin() {
        Map<String, String> body = Map.of("email", "user@local", "password", "pass");

        when(restTemplate.exchange(
                eq("http://identity-service/auth/login"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class))
        ).thenReturn(ResponseEntity.ok("OK"));

        client.send("identity-service", "/auth/login", HttpMethod.POST, Map.of(), null, body, String.class);

        ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://identity-service/auth/login"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(String.class)
        );

        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst("X-Internal-Token")).isEqualTo("test-secret");
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }
}
