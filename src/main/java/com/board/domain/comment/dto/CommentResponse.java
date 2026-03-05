package com.board.domain.comment.dto;

import com.board.domain.comment.entity.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentResponse {

    private Long id;
    private String content;
    private String authorNickname;  // N+1 발생 지점: comment.getMember() lazy load
    private LocalDateTime createdAt;

    public static CommentResponse from(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.id = comment.getId();
        response.content = comment.getContent();
        response.authorNickname = comment.getMember().getNickname();
        response.createdAt = comment.getCreatedAt();
        return response;
    }
}
