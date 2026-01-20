package pl.co.assessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import pl.co.assessment.dto.ExamDraftChangeRequest;
import pl.co.assessment.dto.ExamDraftMetadataRequest;
import pl.co.assessment.dto.ExamDraftSaveRequest;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.service.ExamService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.web.AccessDeniedHandler;
import pl.co.common.web.AuthenticationEntryPoint;
import pl.co.common.web.GlobalExceptionHandler;
import pl.co.common.web.TrimBindingAdvice;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExamController.class)
@Import({GlobalExceptionHandler.class, TrimBindingAdvice.class, ExamControllerTest.TestSecurityConfig.class})
class ExamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExamService examService;

    @Test
    void saveDraft_returns_401_when_unauthenticated() throws Exception {
        ExamDraftSaveRequest request = validRequest();

        mockMvc.perform(post("/exams/{examId}/draft/save", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.UNAUTHORIZED.code()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void saveDraft_returns_403_when_not_admin() throws Exception {
        ExamDraftSaveRequest request = validRequest();

        mockMvc.perform(post("/exams/{examId}/draft/save", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.FORBIDDEN.code()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveDraft_returns_200_when_admin_and_valid() throws Exception {
        ExamDraftSaveRequest request = validRequest();
        doNothing().when(examService).saveDraft(eq("exam-1"), any(ExamDraftSaveRequest.class));

        mockMvc.perform(post("/exams/{examId}/draft/save", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(examService).saveDraft(eq("exam-1"), any(ExamDraftSaveRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveDraft_accepts_metadata_only() throws Exception {
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setMetadata(validMetadata());

        mockMvc.perform(post("/exams/{examId}/draft/save", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(examService).saveDraft(eq("exam-1"), any(ExamDraftSaveRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveDraft_returns_validation_error_when_question_id_missing() throws Exception {
        ExamDraftSaveRequest request = validRequest();
        request.getQuestionChanges().get(0).setQuestionId(null);

        mockMvc.perform(post("/exams/{examId}/draft/save", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.E243.code()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveDraft_returns_validation_error_when_question_id_blank() throws Exception {
        ExamDraftSaveRequest request = validRequest();
        request.getQuestionChanges().get(0).setQuestionId("   ");

        mockMvc.perform(post("/exams/{examId}/draft/save", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.E243.code()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveDraft_returns_invalid_type_when_change_type_invalid() throws Exception {
        String payload = """
                {
                  "questionChanges": [
                    {
                      "questionId": "q1",
                      "questionOrder": 1,
                      "deleted": "INVALID",
                      "type": "SINGLE_CHOICE",
                      "questionContent": {
                        "options": [
                          { "id": "A", "content": "Answer A" }
                        ]
                      },
                      "gradingRules": {
                        "choice": {
                          "correct_option_ids": ["A"]
                        }
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/exams/{examId}/draft/save", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.E202.code()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveDraft_returns_service_error_when_service_throws() throws Exception {
        ExamDraftSaveRequest request = validRequest();
        doThrow(new ApiException(ErrorCode.E227, "Exam not found"))
                .when(examService).saveDraft(eq("exam-1"), any(ExamDraftSaveRequest.class));

        mockMvc.perform(post("/exams/{examId}/draft/save", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.E227.code()));
    }

    @Test
    void discardDraft_returns_401_when_unauthenticated() throws Exception {
        mockMvc.perform(post("/exams/{examId}/draft/discard", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.UNAUTHORIZED.code()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void discardDraft_returns_403_when_not_admin() throws Exception {
        mockMvc.perform(post("/exams/{examId}/draft/discard", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.FORBIDDEN.code()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void discardDraft_returns_200_when_admin() throws Exception {
        doNothing().when(examService).discardDraft("exam-1");

        mockMvc.perform(post("/exams/{examId}/draft/discard", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(examService).discardDraft("exam-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void discardDraft_returns_service_error_when_service_throws() throws Exception {
        doThrow(new ApiException(ErrorCode.E420, "Draft exam version does not exist"))
                .when(examService).discardDraft("exam-1");

        mockMvc.perform(post("/exams/{examId}/draft/discard", "exam-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.E420.code()));
    }

    private ExamDraftSaveRequest validRequest() {
        ExamDraftSaveRequest request = new ExamDraftSaveRequest();
        request.setQuestionChanges(List.of(validChange()));
        return request;
    }

    private ExamDraftChangeRequest validChange() {
        ExamDraftChangeRequest change = new ExamDraftChangeRequest();
        change.setQuestionId("q1");
        change.setQuestionOrder(1);
        change.setDeleted(false);
        change.setType("SINGLE_CHOICE");
        change.setQuestionContent(contentWithOptions());
        change.setGradingRules(rulesWithChoice());
        return change;
    }

    private ExamDraftMetadataRequest validMetadata() {
        ExamDraftMetadataRequest metadata = new ExamDraftMetadataRequest();
        metadata.setName("Sample Exam");
        metadata.setDescription("Draft description");
        metadata.setDurationMinutes(30);
        metadata.setShuffleQuestions(true);
        metadata.setShuffleOptions(false);
        return metadata;
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

    private GradingRules rulesWithChoice() {
        GradingRules.Choice choice = new GradingRules.Choice();
        choice.setCorrectOptionIds(List.of("A"));
        GradingRules rules = new GradingRules();
        rules.setChoice(choice);
        return rules;
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                AuthenticationEntryPoint authenticationEntryPoint,
                                                AccessDeniedHandler accessDeniedHandler) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic();
            return http.build();
        }

        @Bean
        AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
            return new AuthenticationEntryPoint(objectMapper);
        }

        @Bean
        AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
            return new AccessDeniedHandler(objectMapper);
        }
    }
}
