package pl.co.storage.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.co.storage.service.FileService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FileCommitListener {

    private static final Logger log = LoggerFactory.getLogger(FileCommitListener.class);
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final FileService fileService;

    @KafkaListener(topics = "${kafka.topics.file}")
    public void onMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        List<String> fileIds;
        try {
            fileIds = objectMapper.readValue(message, LIST_TYPE);
        } catch (Exception ex) {
            log.warn("Invalid file commit payload: {}", ex.getMessage());
            return;
        }
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (String fileId : fileIds) {
            if (fileId == null || fileId.isBlank()) {
                continue;
            }
            try {
                fileService.commit(fileId);
            } catch (Exception ex) {
                log.warn("Failed to commit file {}: {}", fileId, ex.getMessage());
            }
        }
    }
}
