package pl.co.storage.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.co.storage.dto.FileHlsRequest;
import pl.co.storage.service.FileHlsService;

@Component
@RequiredArgsConstructor
public class FileHlsListener {

    private static final Logger log = LoggerFactory.getLogger(FileHlsListener.class);

    private final ObjectMapper objectMapper;
    private final FileHlsService fileHlsService;

    @KafkaListener(topics = "${kafka.topics.file-hls}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        FileHlsRequest request;
        try {
            request = objectMapper.readValue(message, FileHlsRequest.class);
        } catch (Exception ex) {
            log.warn("Invalid file HLS payload: {}", ex.getMessage());
            return;
        }
        if (request == null || request.getFileId() == null || request.getFileId().isBlank()) {
            return;
        }
        try {
            fileHlsService.processHls(request.getFileId());
        } catch (Exception ex) {
            log.warn("Failed to process HLS for file {}: {}", request.getFileId(), ex.getMessage());
        }
    }
}
