package pl.co.assessment.service;

import pl.co.assessment.dto.ExamSessionAssignmentRequest;
import pl.co.assessment.dto.ExamSessionAssignmentResponse;
import pl.co.assessment.dto.ExamSessionCreateRequest;
import pl.co.assessment.dto.ExamSessionDetailResponse;
import pl.co.assessment.dto.ExamSessionListItemResponse;
import pl.co.assessment.dto.ExamSessionResponse;
import pl.co.assessment.dto.ExamSessionUpdateRequest;

import java.time.Instant;
import java.util.List;

public interface ExamSessionService {

    ExamSessionResponse createSession(String examId, ExamSessionCreateRequest request);

    List<ExamSessionListItemResponse> listSessions(String examId,
                                                   Instant startTime,
                                                   Instant endTime,
                                                   String searchValue,
                                                   Integer page,
                                                   Integer size);

    ExamSessionDetailResponse getSession(String sessionId);

    ExamSessionResponse updateSession(String sessionId, ExamSessionUpdateRequest request);

    ExamSessionResponse rotateSessionCode(String sessionId);

    void deleteSession(String sessionId);

    List<ExamSessionAssignmentResponse> addAssignments(String sessionId, ExamSessionAssignmentRequest request);

    void deleteAssignment(String sessionId, String assignmentId);
}
