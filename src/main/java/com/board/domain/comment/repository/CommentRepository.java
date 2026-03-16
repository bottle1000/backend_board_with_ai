package com.board.domain.comment.repository;

import com.board.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostId(Long postId);

    // Fetch Join: comment 조회 시 member를 한 번에 가져옴 → N+1 해결
    @Query("SELECT c FROM Comment c JOIN FETCH c.member WHERE c.post.id = :postId")
    List<Comment> findByPostIdWithMember(@Param("postId") Long postId);
}
