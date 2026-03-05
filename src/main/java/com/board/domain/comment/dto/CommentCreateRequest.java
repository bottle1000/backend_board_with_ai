package com.board.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CommentCreateRequest {

    @NotBlank(message = "내용은 필수입니다.")
    private String content;
}
