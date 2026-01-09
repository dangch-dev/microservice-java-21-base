package pl.co.storage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.storage.entity.File;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, String> {
    List<File> findByIdInAndStatusAndDeletedFalse(Collection<String> ids, String status);

    Optional<File> findByIdAndDeletedFalse(String id);

    List<File> findByStatusAndUpdatedAtBeforeAndDeletedFalse(String status, Instant updatedBefore);
}
