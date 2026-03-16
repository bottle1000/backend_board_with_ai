package com.board.domain.post.repository;

import com.board.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByTitleContaining(String keyword, Pageable pageable);

    // Fetch Join: post 조회 시 member를 한 번에 가져옴 → N+1 해결
    // countQuery를 별도 분리하는 이유: JOIN FETCH 쿼리에 COUNT를 적용하면 Hibernate가 오류를 냄
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.member",
           countQuery = "SELECT COUNT(p) FROM Post p")
    Page<Post> findAllWithMember(Pageable pageable);
}
