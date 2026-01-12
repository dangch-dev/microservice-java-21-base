package pl.co.common.template;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TemplateLoader {

    public String load(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return "";
        }
        try {
            if (pathOrUrl.startsWith("classpath:")) {
                return loadClasspath(pathOrUrl.substring("classpath:".length()));
            }
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                return loadUrl(pathOrUrl);
            }
            if (pathOrUrl.startsWith("file:")) {
                return loadFile(pathOrUrl.substring("file:".length()));
            }
            return loadClasspath(pathOrUrl);
        } catch (Exception ex) {
            return "";
        }
    }

    private String loadClasspath(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String loadFile(String filePath) throws Exception {
        return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
    }

    private String loadUrl(String url) throws Exception {
        try (InputStream input = URI.create(url).toURL().openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
