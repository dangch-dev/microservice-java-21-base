package pl.co.storage.service;

public interface FileService {
    void commit(String fileId);

    void cleanupPending();
}
