package pl.co.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.co.assessment.entity.ExamSessionTargetType;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ExamSessionCreateRequest {

    @NotBlank
    private String title;

    private Instant startAt;

    private Instant endAt;

    @NotNull
    private ExamSessionTargetType targetType;

    private String accessCode;

    private List<String> userIds;

    private Integer guestCount;

    @Valid
    private List<GuestInfoRequest> guestInfo;
}
