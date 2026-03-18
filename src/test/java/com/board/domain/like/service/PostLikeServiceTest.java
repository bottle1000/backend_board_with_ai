package com.board.domain.like.service;

import com.board.domain.like.entity.PostLike;
import com.board.domain.like.repository.PostLikeRepository;
import com.board.domain.member.entity.Member;
import com.board.domain.member.repository.MemberRepository;
import com.board.domain.post.entity.Post;
import com.board.domain.post.repository.PostRepository;
import com.board.global.security.CustomUserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class PostLikeServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    PostLikeRepository postLikeRepository;

    @InjectMocks
    PostLikeService postLikeService;

    private Member member;
    private Post post;
    private PostLike postLike;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("test@test.com")
                .password("test1234")
                .nickname("tester1")
                .build();

        post = Post.builder()
                .title("test title")
                .content("test content")
                .member(member)
                .build();

        postLike = PostLike.builder()
                .post(post)
                .member(member)
                .build();

        userDetails = new CustomUserDetails(1L, "test@test.com", "test1234");
    }

    @Test
    @DisplayName("좋아요 등록 기능이 정상적으로 작동 됐을 때")
    void toggleLike_등록성공(){
        // given
        given(postRepository.findByIdWithLock(1L)).willReturn(Optional.of(post));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postLikeRepository.findByPostIdAndMemberId(1L, 1L)).willReturn(Optional.empty());

        // when
        String result = postLikeService.toggleLike(1L, userDetails);

        // then
        assertThat(result).isEqualTo("좋아요가 등록되었습니다.");
        assertThat(post.getLikeCount()).isEqualTo(1);
        verify(postLikeRepository, times(1)).save(any(PostLike.class));

    }

    @Test
    @DisplayName("좋아요 취소 기능이 정상적으로 작동 됐을 때")
    void toggleLike_취소성공() {
        // given: 이미 좋아요가 1개 눌린 상태에서 시작
        ReflectionTestUtils.setField(post, "likeCount", 1);
        given(postRepository.findByIdWithLock(1L)).willReturn(Optional.of(post));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postLikeRepository.findByPostIdAndMemberId(1L, 1L)).willReturn(Optional.of(postLike));

        // when
        String result = postLikeService.toggleLike(1L, userDetails);

        //then
        assertThat(result).isEqualTo("좋아요가 취소되었습니다.");
        assertThat(post.getLikeCount()).isEqualTo(0);
        verify(postLikeRepository, times(1)).delete(postLike);
    }

}
