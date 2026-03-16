package com.board.domain.post.repository;

import com.board.domain.post.entity.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByTitleContaining(String keyword, Pageable pageable);

    // 비관적 락: 좋아요 처리 시 다른 트랜잭션이 같은 행을 수정하지 못하도록 잠금
    // SELECT ... FOR UPDATE → 트랜잭션 종료 전까지 해당 행 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdWithLock(Long id);

    // Fetch Join: post 조회 시 member를 한 번에 가져옴 → N+1 해결
    // countQuery를 별도 분리하는 이유: JOIN FETCH 쿼리에 COUNT를 적용하면 Hibernate가 오류를 냄
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.member",
           countQuery = "SELECT COUNT(p) FROM Post p")
    Page<Post> findAllWithMember(Pageable pageable);
}
