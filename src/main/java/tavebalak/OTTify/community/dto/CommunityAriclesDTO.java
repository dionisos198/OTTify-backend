package tavebalak.OTTify.community.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tavebalak.OTTify.community.entity.Reply;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class CommunityAriclesDTO {
    private String title;
    private String writer;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int commentAmount;
    private int likeCount;
    private Long subjectId;
    private List<CommentListsDTO> commentListsDTOList;

    @Builder
    public CommunityAriclesDTO(String title,
                               String writer,
                               Long userId,
                               String content,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt,
                               int commentAmount,
                               List<CommentListsDTO> commentListsDTOList,
                               int likeCount,
                               Long subjectId
    ) {
        this.title = title;
        this.writer = writer;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.commentAmount = commentAmount;
        this.commentListsDTOList = commentListsDTOList;
        this.likeCount = likeCount;
        this.subjectId = subjectId;
    }
}