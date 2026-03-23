package pl.co.storage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.util.StringUtils;
import pl.co.storage.dto.FileDownload;
import pl.co.storage.entity.File;
import pl.co.storage.entity.HlsStatus;
import pl.co.storage.repository.FileRepository;
import pl.co.storage.service.FileHlsService;
import pl.co.storage.service.ObjectStorageService;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileHlsServiceImpl implements FileHlsService {

    private static final String HLS_DIR = "hls";
    private static final String HLS_PLAYLIST = "index.m3u8";
    private static final int HLS_SEGMENT_SECONDS = 6;
    private static final String HLS_PLAYLIST_CONTENT_TYPE = "application/vnd.apple.mpegurl";
    private static final String HLS_SEGMENT_CONTENT_TYPE = "video/mp2t";

    private final FileRepository fileRepository;
    private final ObjectStorageService objectStorageService;
    @Qualifier("hlsExecutor")
    private final TaskExecutor hlsExecutor;

    @Override
    public FileDownload downloadHlsPlaylist(String fileId) {
        File file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "File not found"));
        if (!isHlsReady(file)) {
            return null;
        }
        if (!StringUtils.hasText(file.getHlsPlaylistKey())) {
            throw new ApiException(ErrorCode.E227, "HLS playlist not ready");
        }
        return downloadByObjectKey(file.getHlsPlaylistKey(), HLS_PLAYLIST_CONTENT_TYPE, HLS_PLAYLIST);
    }

    @Override
    public FileDownload downloadHlsSegment(String fileId, String segmentName) {
        File file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "File not found"));
        if (!isHlsReady(file)) {
            return null;
        }
        String objectKey = buildHlsObjectKey(fileId, segmentName);
        return downloadByObjectKey(objectKey, HLS_SEGMENT_CONTENT_TYPE, segmentName);
    }

    @Override
    public void processHls(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            return;
        }
        File file = fileRepository.findByIdAndDeletedFalse(fileId).orElse(null);
        if (file == null || !isVideo(file.getMimeType())) {
            return;
        }
        if (isHlsReady(file)) {
            return;
        }
        if (HlsStatus.PROCESSING.name().equalsIgnoreCase(file.getHlsStatus())) {
            return;
        }
        file.setHlsStatus(HlsStatus.PROCESSING.name());
        fileRepository.save(file);
        try {
            hlsExecutor.execute(() -> handleHls(fileId));
        } catch (RejectedExecutionException ex) {
            log.warn("HLS executor queue is full. Skipping file {}: {}", file.getId(), ex.getMessage());
            file.setHlsStatus(HlsStatus.FAILED.name());
            fileRepository.save(file);
        }
    }

    private void handleHls(String fileId) {
        File file = fileRepository.findByIdAndDeletedFalse(fileId).orElse(null);
        if (file == null || !isVideo(file.getMimeType())) {
            return;
        }
        if (!HlsStatus.PROCESSING.name().equalsIgnoreCase(file.getHlsStatus())) {
            return;
        }
        try {
            String playlistKey = generateHlsAssets(file);
            file.setHlsPlaylistKey(playlistKey);
            file.setHlsStatus(HlsStatus.READY.name());
        } catch (Exception ex) {
            log.error("HLS pipeline failed for file {}: {}", file.getId(), ex.getMessage());
            file.setHlsStatus(HlsStatus.FAILED.name());
        }
        fileRepository.save(file);
    }

    private boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.toLowerCase().startsWith("video/");
    }

    private boolean isHlsReady(File file) {
        return file != null && HlsStatus.READY.name().equalsIgnoreCase(file.getHlsStatus());
    }

    private String generateHlsAssets(File file) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("hls-" + file.getId() + "-");
        Path inputFile = workDir.resolve("source");
        Path outputDir = workDir.resolve(HLS_DIR);
        Files.createDirectories(outputDir);
        try (InputStream stream = objectStorageService.getObject(file.getObjectKey());
             BufferedInputStream buffered = new BufferedInputStream(stream)) {
            Files.copy(buffered, inputFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            cleanupQuietly(workDir);
            throw new ApiException(ErrorCode.E281, "Unable to download source for HLS", ex);
        }

        String segmentPattern = outputDir.resolve("seg_%03d.ts").toString();
        String playlistPath = outputDir.resolve(HLS_PLAYLIST).toString();

        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.toString(),
                "-loglevel", "error",
                "-c:v", "copy",
                "-c:a", "copy",
                "-hls_time", String.valueOf(HLS_SEGMENT_SECONDS),
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern,
                "-f", "hls",
                playlistPath
        );
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (InputStream processOut = process.getInputStream()) {
            processOut.transferTo(OutputStream.nullOutputStream());
        }
        int exit = process.waitFor();
        if (exit != 0) {
            cleanupQuietly(workDir);
            throw new ApiException(ErrorCode.E281, "ffmpeg failed with exit code " + exit);
        }

        String prefix = buildHlsPrefix(file.getId());
        try (var paths = Files.list(outputDir)) {
            paths.forEach(path -> {
                String filename = path.getFileName().toString();
                String contentType = filename.endsWith(".m3u8") ? HLS_PLAYLIST_CONTENT_TYPE : HLS_SEGMENT_CONTENT_TYPE;
                String objectKey = prefix + filename;
                try (InputStream inputStream = Files.newInputStream(path)) {
                    objectStorageService.putObject(objectKey, inputStream, Files.size(path), contentType);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (RuntimeException ex) {
            cleanupQuietly(workDir);
            throw new ApiException(ErrorCode.E281, "Unable to upload HLS assets", ex);
        } finally {
            cleanupQuietly(workDir);
        }
        return prefix + HLS_PLAYLIST;
    }

    private String buildHlsPrefix(String fileId) {
        return "files/" + fileId + "/" + HLS_DIR + "/";
    }

    private String buildHlsObjectKey(String fileId, String segmentName) {
        return buildHlsPrefix(fileId) + segmentName;
    }

    private FileDownload downloadByObjectKey(String objectKey, String contentType, String filename) {
        try {
            InputStream stream = objectStorageService.getObject(objectKey);
            var statResponse = objectStorageService.statObject(objectKey);
            Long size = statResponse.map(r -> r.size()).orElse(null);
            return new FileDownload(stream, filename, contentType, size);
        } catch (Exception ex) {
            log.error("Failed to get object {}: {}", objectKey, ex.getMessage());
            throw new ApiException(ErrorCode.E281, "Unable to download file", ex);
        }
    }

    private void cleanupQuietly(Path workDir) {
        if (workDir == null) {
            return;
        }
        try {
            if (Files.exists(workDir)) {
                Files.walk(workDir)
                        .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }
}
