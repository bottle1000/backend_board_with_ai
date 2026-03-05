package com.board.domain.like.service;

import com.board.domain.like.entity.PostLike;
import com.board.domain.like.repository.PostLikeRepository;
import com.board.domain.member.entity.Member;
import com.board.domain.member.repository.MemberRepository;
import com.board.domain.post.entity.Post;
import com.board.domain.post.repository.PostRepository;
import com.board.global.exception.CustomException;
import com.board.global.exception.ErrorCode;
import com.board.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    // 동시성 제어 없음 — 의도적으로 race condition 허용 (동시성 테스트용)
    @Transactional
    public String toggleLike(Long postId, CustomUserDetails userDetails) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        Member member = memberRepository.findById(userDetails.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, userDetails.getId());

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.decreaseLikeCount();
            return "좋아요가 취소되었습니다.";
        } else {
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .member(member)
                    .build();
            postLikeRepository.save(postLike);
            post.increaseLikeCount();
            return "좋아요가 등록되었습니다.";
        }
    }
}
