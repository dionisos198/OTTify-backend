package tavebalak.OTTify.community.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplyCommentCreateDTO {
    private Long subjectId;
    @NotBlank(message = "댓글 내용은 비어있어서는 안됩니다.")
    private String comment;

    @Builder
    public ReplyCommentCreateDTO(Long subjectId, String comment) {
        this.subjectId = subjectId;
        this.comment = comment;
    }
}