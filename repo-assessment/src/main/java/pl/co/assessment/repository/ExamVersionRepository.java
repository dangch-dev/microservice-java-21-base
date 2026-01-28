package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.co.assessment.entity.ExamVersion;

import java.util.List;
import java.util.Optional;

public interface ExamVersionRepository extends JpaRepository<ExamVersion, String> {

    boolean existsByIdAndExamIdAndDeletedFalse(String id, String examId);

    Optional<ExamVersion> findByIdAndExamIdAndDeletedFalse(String id, String examId);

    Optional<ExamVersion> findByIdAndDeletedFalse(String id);

    List<ExamVersion> findByIdInAndDeletedFalse(List<String> ids);

    @Query("select coalesce(max(ev.version), 0) from ExamVersion ev where ev.examId = :examId and ev.deleted = false")
    Integer findMaxVersionByExamIdAndDeletedFalse(@Param("examId") String examId);

}
