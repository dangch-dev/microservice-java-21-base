package pl.co.assessment.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.co.assessment.dto.ExamDraftChangeRequest;
import pl.co.assessment.dto.ExamDraftMetadataRequest;
import pl.co.assessment.dto.ExamDraftSaveRequest;
import pl.co.assessment.entity.Exam;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.repository.ExamRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.repository.QuestionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.service.QuestionService;
import pl.co.assessment.service.impl.QuestionServiceImpl;
import pl.co.common.file.FilePublisher;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamVersionRepository examVersionRepository;
    @Mock
    private ExamVersionQuestionRepository examVersionQuestionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionVersionRepository questionVersionRepository;
    @Mock
    private FilePublisher filePublisher;

    private QuestionService questionService;
    private ExamServiceImpl examService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionServiceImpl();
        examService = new ExamServiceImpl(
                examRepository,
                examVersionRepository,
                examVersionQuestionRepository,
                questionRepository,
                questionVersionRepository,
                questionService,
                filePublisher
        );
        lenient().when(questionVersionRepository.save(any(QuestionVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveDraft_throws_when_payload_empty() {
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", request));

        assertEquals(ErrorCode.E221, ex.getErrorCode());
    }

    @Test
    void saveDraft_throws_when_exam_not_found() {
        ExamDraftSaveRequest request = saveRequest(change("q1", 1, "SINGLE_CHOICE", false,
                contentWithOptions(), rulesWithChoice()));
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", request));

        assertEquals(ErrorCode.E227, ex.getErrorCode());
    }

    @Test
    void saveDraft_throws_when_draft_missing() {
        Exam exam = examWithDraft(null);
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "SINGLE_CHOICE",
                        false, contentWithOptions(), rulesWithChoice()))));

        assertEquals(ErrorCode.E420, ex.getErrorCode());
    }

    @Test
    void saveDraft_throws_when_draft_not_found() {
        Exam exam = examWithDraft("draft-1");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "SINGLE_CHOICE",
                        false, contentWithOptions(), rulesWithChoice()))));

        assertEquals(ErrorCode.E420, ex.getErrorCode());
    }

    @Test
    void saveDraft_throws_when_draft_status_invalid() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "published");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "SINGLE_CHOICE",
                        false, contentWithOptions(), rulesWithChoice()))));

        assertEquals(ErrorCode.E420, ex.getErrorCode());
    }

    @Test
    void saveDraft_throws_when_delete_question_missing() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(mapping("evq-1", "draft-1", "q1", "qv-1", 1)));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q2", 1, "SINGLE_CHOICE",
                        true, contentWithOptions(), rulesWithChoice()))));

        assertEquals(ErrorCode.E221, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q2"));
    }

    @Test
    void saveDraft_throws_when_add_duplicate_order() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(mapping("evq-1", "draft-1", "q1", "qv-1", 1)));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q2", 1, "SINGLE_CHOICE",
                        false, contentWithOptions(), rulesWithChoice()))));

        assertEquals(ErrorCode.E220, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionOrder"));
    }

    @Test
    void saveDraft_throws_when_invalid_type() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "INVALID",
                        false, contentWithOptions(), rulesWithChoice()))));

        assertEquals(ErrorCode.E221, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_single_choice_missing_options() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        QuestionContent content = new QuestionContent();
        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "SINGLE_CHOICE",
                        false, content, rulesWithChoice()))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_single_choice_missing_choice_rules() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        GradingRules rules = new GradingRules();
        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "SINGLE_CHOICE",
                        false, contentWithOptions(), rules))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_single_choice_missing_correct_options() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        GradingRules rules = new GradingRules();
        GradingRules.Choice choice = new GradingRules.Choice();
        choice.setCorrectOptionIds(List.of());
        rules.setChoice(choice);

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "SINGLE_CHOICE",
                        false, contentWithOptions(), rules))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_multiple_choice_missing_correct_options() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        GradingRules rules = new GradingRules();
        GradingRules.Choice choice = new GradingRules.Choice();
        choice.setCorrectOptionIds(List.of());
        rules.setChoice(choice);

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "MULTIPLE_CHOICE",
                        false, contentWithOptions(), rules))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_short_text_missing_rules() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "SHORT_TEXT",
                        false, new QuestionContent(), new GradingRules()))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_matching_missing_payload() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "MATCHING",
                        false, new QuestionContent(), new GradingRules()))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_matching_missing_rules() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        QuestionContent content = contentWithMatching();
        GradingRules rules = new GradingRules();

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "MATCHING",
                        false, content, rules))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_fill_blanks_missing_payload() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "FILL_BLANKS",
                        false, new QuestionContent(), new GradingRules()))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_fill_blanks_missing_rules() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        QuestionContent content = contentWithBlanks();
        GradingRules rules = new GradingRules();

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "FILL_BLANKS",
                        false, content, rules))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_edit_invalid_type() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(mapping("evq-1", "draft-1", "q1", "qv-1", 1)));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "INVALID",
                        false, contentWithOptions(), rulesWithChoice()))));

        assertEquals(ErrorCode.E221, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_EDIT_order_collides_with_existing() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(
                        mapping("evq-1", "draft-1", "q1", "qv-1", 1),
                        mapping("evq-2", "draft-1", "q2", "qv-2", 2)
                ));

        ExamDraftChangeRequest change1 = change("q1", 2, "SINGLE_CHOICE",
                false, contentWithOptions(), rulesWithChoice());
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setQuestionChanges(List.of(change1));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", request));

        assertEquals(ErrorCode.E220, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionOrder"));
    }

    @Test
    void saveDraft_throws_when_EDIT_duplicate_question_id() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(mapping("evq-1", "draft-1", "q1", "qv-1", 1)));

        ExamDraftChangeRequest change1 = change("q1", 2, "SINGLE_CHOICE",
                false, contentWithOptions(), rulesWithChoice());
        ExamDraftChangeRequest change2 = change("q1", 3, "SINGLE_CHOICE",
                false, contentWithOptions(), rulesWithChoice());
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setQuestionChanges(List.of(change1, change2));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", request));

        assertEquals(ErrorCode.E220, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_throws_when_EDIT_duplicate_order() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(
                        mapping("evq-1", "draft-1", "q1", "qv-1", 1),
                        mapping("evq-2", "draft-1", "q2", "qv-2", 2)
                ));

        ExamDraftChangeRequest change1 = change("q1", 3, "SINGLE_CHOICE",
                false, contentWithOptions(), rulesWithChoice());
        ExamDraftChangeRequest change2 = change("q2", 3, "SINGLE_CHOICE",
                false, contentWithOptions(), rulesWithChoice());
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setQuestionChanges(List.of(change1, change2));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", request));

        assertEquals(ErrorCode.E220, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionOrder"));
    }

    @Test
    void saveDraft_throws_when_order_not_continuous_after_delete() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(
                        mapping("evq-1", "draft-1", "q1", "qv-1", 1),
                        mapping("evq-2", "draft-1", "q2", "qv-2", 2)
                ));

        ExamDraftSaveRequest request = saveRequest(change("q1", 1, "SINGLE_CHOICE",
                true, contentWithOptions(), rulesWithChoice()));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", request));

        assertEquals(ErrorCode.E221, ex.getErrorCode());
    }

    @Test
    void saveDraft_updates_EDIT_orders_in_single_request() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        ExamVersionQuestion q1 = mapping("evq-1", "draft-1", "q1", "qv-1", 1);
        ExamVersionQuestion q2 = mapping("evq-2", "draft-1", "q2", "qv-2", 2);
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(q1, q2));

        ExamDraftChangeRequest edit1 = change("q1", 2, "SINGLE_CHOICE",
                false, contentWithOptions(), rulesWithChoice());
        ExamDraftChangeRequest edit2 = change("q2", 1, "SINGLE_CHOICE",
                false, contentWithOptions(), rulesWithChoice());
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setQuestionChanges(List.of(edit1, edit2));

        examService.saveDraft("exam-1", request);

        ArgumentCaptor<Iterable<ExamVersionQuestion>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(examVersionQuestionRepository).saveAll(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void saveDraft_updates_REORDER_ONLY_orders_without_new_version() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        ExamVersionQuestion q1 = mapping("evq-1", "draft-1", "q1", "qv-1", 1);
        ExamVersionQuestion q2 = mapping("evq-2", "draft-1", "q2", "qv-2", 2);
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(q1, q2));

        ExamDraftChangeRequest reorder1 = reorderOnlyChange("q1", 2);
        ExamDraftChangeRequest reorder2 = reorderOnlyChange("q2", 1);
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setQuestionChanges(List.of(reorder1, reorder2));

        examService.saveDraft("exam-1", request);

        assertEquals(2, q1.getQuestionOrder());
        assertEquals(1, q2.getQuestionOrder());
        verify(questionVersionRepository, never()).save(any());
        verify(examVersionQuestionRepository).saveAll(any());
    }

    @Test
    void saveDraft_throws_when_manual_rubric_exceeds_max_points() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        GradingRules rules = new GradingRules();
        rules.setMaxPoints(new BigDecimal("5"));
        GradingRules.Manual manual = new GradingRules.Manual();
        GradingRules.RubricItem item1 = new GradingRules.RubricItem();
        item1.setMaxPoints(new BigDecimal("3"));
        GradingRules.RubricItem item2 = new GradingRules.RubricItem();
        item2.setMaxPoints(new BigDecimal("3"));
        manual.setRubric(List.of(item1, item2));
        rules.setManual(manual);

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.saveDraft("exam-1", saveRequest(change("q1", 1, "ESSAY",
                        false, contentWithOptions(), rules))));

        assertEquals(ErrorCode.E204, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("questionId: q1"));
    }

    @Test
    void saveDraft_updates_metadata_only() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of());

        ExamDraftMetadataRequest metadata = new ExamDraftMetadataRequest();
        metadata.setName("Updated name");
        metadata.setDescription("Updated description");
        metadata.setDurationMinutes(45);
        metadata.setShuffleQuestions(true);
        metadata.setShuffleOptions(false);
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setMetadata(metadata);

        examService.saveDraft("exam-1", request);

        assertEquals("Updated name", draft.getName());
        assertEquals("Updated description", draft.getDescription());
        assertEquals(45, draft.getDurationMinutes());
        assertTrue(draft.isShuffleQuestions());
        assertTrue(!draft.isShuffleOptions());
        verify(examVersionRepository).save(draft);
    }

    @Test
    void saveDraft_updates_EDIT_and_persists_changes() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));
        ExamVersionQuestion q1 = mapping("evq-1", "draft-1", "q1", "qv-1", 1);
        when(examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc("draft-1"))
                .thenReturn(List.of(q1));

        ExamDraftChangeRequest EDIT = change("q1", 1, "SINGLE_CHOICE",
                false, contentWithOptions(), rulesWithChoice());
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setQuestionChanges(List.of(EDIT));

        examService.saveDraft("exam-1", request);

        ArgumentCaptor<Iterable<ExamVersionQuestion>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(examVersionQuestionRepository).saveAll(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void discardDraft_throws_when_exam_not_found() {
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.discardDraft("exam-1"));

        assertEquals(ErrorCode.E227, ex.getErrorCode());
    }

    @Test
    void discardDraft_returns_when_draft_missing() {
        Exam exam = examWithDraft(null);
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));

        examService.discardDraft("exam-1");

        verify(examVersionRepository, never()).findByIdAndExamIdAndDeletedFalse(anyString(), anyString());
        verify(examVersionRepository, never()).save(any());
        verify(examRepository, never()).save(any());
        assertNull(exam.getDraftExamVersionId());
    }

    @Test
    void discardDraft_throws_when_draft_not_found() {
        Exam exam = examWithDraft("draft-1");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.discardDraft("exam-1"));

        assertEquals(ErrorCode.E420, ex.getErrorCode());
    }

    @Test
    void discardDraft_throws_when_draft_status_invalid() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "published");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));

        ApiException ex = assertThrows(ApiException.class,
                () -> examService.discardDraft("exam-1"));

        assertEquals(ErrorCode.E420, ex.getErrorCode());
    }

    @Test
    void discardDraft_soft_deletes_and_clears_pointer() {
        Exam exam = examWithDraft("draft-1");
        ExamVersion draft = draftVersion("exam-1", "draft");
        when(examRepository.findByIdAndDeletedFalseForUpdate("exam-1")).thenReturn(Optional.of(exam));
        when(examVersionRepository.findByIdAndExamIdAndDeletedFalse("draft-1", "exam-1"))
                .thenReturn(Optional.of(draft));

        examService.discardDraft("exam-1");

        assertTrue(draft.isDeleted());
        assertNull(exam.getDraftExamVersionId());
        verify(examVersionRepository).save(draft);
        verify(examRepository).save(exam);
    }

    private Exam examWithDraft(String draftId) {
        Exam exam = Exam.builder().draftExamVersionId(draftId).build();
        exam.setId("exam-1");
        return exam;
    }

    private ExamVersion draftVersion(String examId, String status) {
        ExamVersion draft = ExamVersion.builder()
                .examId(examId)
                .version(1)
                .name("Draft")
                .status(status)
                .build();
        draft.setId("draft-1");
        return draft;
    }

    private ExamVersionQuestion mapping(String id, String draftId, String questionId, String qvId, int order) {
        ExamVersionQuestion mapping = ExamVersionQuestion.builder()
                .examVersionId(draftId)
                .questionId(questionId)
                .questionVersionId(qvId)
                .questionOrder(order)
                .build();
        mapping.setId(id);
        return mapping;
    }

    private ExamDraftSaveRequest saveRequest(ExamDraftChangeRequest change) {
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setQuestionChanges(List.of(change));
        return request;
    }

    private ExamDraftChangeRequest change(String questionId, int order, String type,
                                          boolean deleted,
                                          QuestionContent content,
                                          GradingRules rules) {
        ExamDraftChangeRequest change = new ExamDraftChangeRequest();
        change.setQuestionId(questionId);
        change.setQuestionOrder(order);
        change.setType(type);
        change.setDeleted(deleted);
        change.setQuestionContent(content);
        change.setGradingRules(rules);
        return change;
    }

    private ExamDraftChangeRequest reorderOnlyChange(String questionId, int order) {
        ExamDraftChangeRequest change = new ExamDraftChangeRequest();
        change.setQuestionId(questionId);
        change.setQuestionOrder(order);
        change.setDeleted(false);
        return change;
    }

    private QuestionContent contentWithOptions() {
        QuestionContent.Option option = QuestionContent.Option.builder()
                .id("A")
                .content("Answer A")
                .build();
        QuestionContent content = new QuestionContent();
        content.setOptions(List.of(option));
        return content;
    }

    private QuestionContent contentWithMatching() {
        QuestionContent.Item left = QuestionContent.Item.builder()
                .id("L1")
                .content("Left")
                .build();
        QuestionContent.Item right = QuestionContent.Item.builder()
                .id("R1")
                .content("Right")
                .build();
        QuestionContent.Matching matching = new QuestionContent.Matching();
        matching.setLeftItems(List.of(left));
        matching.setRightItems(List.of(right));
        QuestionContent content = new QuestionContent();
        content.setMatching(matching);
        return content;
    }

    private QuestionContent contentWithBlanks() {
        QuestionContent.Blanks blanks = new QuestionContent.Blanks();
        blanks.setInputKind("text");
        QuestionContent content = new QuestionContent();
        content.setBlanks(blanks);
        return content;
    }

    private GradingRules rulesWithChoice() {
        GradingRules.Choice choice = new GradingRules.Choice();
        choice.setCorrectOptionIds(List.of("A"));
        GradingRules rules = new GradingRules();
        rules.setChoice(choice);
        return rules;
    }
}
