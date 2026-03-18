package com.board.domain.post.service;

import com.board.domain.member.entity.Member;
import com.board.domain.member.repository.MemberRepository;
import com.board.domain.post.dto.PostCreateRequest;
import com.board.domain.post.dto.PostResponse;
import com.board.domain.post.dto.PostUpdateRequest;
import com.board.domain.post.entity.Post;
import com.board.domain.post.repository.PostRepository;
import com.board.global.exception.CustomException;
import com.board.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)  // Mockito를 JUnit5에서 사용하도록 활성화
class PostServiceTest {

    @Mock
    PostRepository postRepository;      // 가짜 PostRepository

    @Mock
    MemberRepository memberRepository;  // 가짜 MemberRepository

    @InjectMocks
    PostService postService;            // 위의 가짜들을 주입받은 진짜 PostService

    // 테스트에서 공통으로 사용할 객체
    private Member member;
    private Post post;
    private CustomUserDetails userDetails;

    @BeforeEach  // 각 테스트 실행 전에 매번 호출됨
    void setUp() {
        member = Member.builder()
                .email("test@test.com")
                .password("password123")
                .nickname("테스터")
                .build();

        // Member의 id는 DB 자동생성이라 직접 설정 불가
        // ReflectionTestUtils: 테스트에서 private 필드 값을 설정할 때 사용하는 Spring 유틸
        ReflectionTestUtils.setField(member, "id", 1L);

        post = Post.builder()
                .member(member)
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        ReflectionTestUtils.setField(post, "id", 1L);

        userDetails = new CustomUserDetails(1L, "test@test.com", "password123");
    }

    // ======================== getPost 테스트 ========================

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void getPost_성공() {
        // given: postRepository가 post를 반환하도록 설정
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when: 실제 메서드 호출
        PostResponse result = postService.getPost(1L);

        // then: 결과 검증
        assertThat(result.getTitle()).isEqualTo("테스트 제목");
        assertThat(result.getAuthorNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외 발생")
    void getPost_없는게시글_예외() {
        // given: 아무것도 없는 빈 결과 반환
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then: CustomException이 발생하는지 검증
        assertThatThrownBy(() -> postService.getPost(999L))
                .isInstanceOf(CustomException.class);
    }

    // ======================== createPost 테스트 ========================

    @Test
    @DisplayName("게시글 생성 성공")
    void createPost_성공() {
        // given
        PostCreateRequest request = new PostCreateRequest("새 제목", "새 내용");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);

        // when
        PostResponse result = postService.createPost(userDetails, request);

        // then
        assertThat(result.getTitle()).isEqualTo("테스트 제목");
        // save()가 실제로 호출됐는지 검증
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 시 없는 회원이면 예외 발생")
    void createPost_없는회원_예외() {
        //given
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                postService.createPost(userDetails, new PostCreateRequest("제목", "내용")))
                .isInstanceOf(CustomException.class);
    }

    // ======================== updatePost 테스트 ========================

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_성공() {
        // given
        PostUpdateRequest request = new PostUpdateRequest("수정된 제목", "수정된 내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when
        PostResponse result = postService.updatePost(1L, userDetails, request);

        // then
        assertThat(result.getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("다른 사람의 게시글 수정 시도 시 예외 발생")
    void updatePost_타인게시글_예외() {
        // given: 게시글 작성자는 ID=1, 요청자는 ID=2 (다른 사람)
        PostUpdateRequest request = new PostUpdateRequest("수정된 제목", "수정된 내용");
        CustomUserDetails anotherUser = new CustomUserDetails(2L, "other@test.com", "password");

        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when & then: 권한 없음 예외 발생 검증
        assertThatThrownBy(() -> postService.updatePost(1L, anotherUser, request))
                .isInstanceOf(CustomException.class);
    }

    // ======================== deletePost 테스트 ========================

    @Test
    @DisplayName("게시글 성공적으로 삭제")
    void deletePost_성공() {
        // given
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when
        postService.deletePost(post.getId(), userDetails);

        //then
        verify(postRepository, times(1)).delete(post);

    }

    @Test
    @DisplayName("다른 사람의 게시글 삭제 시도 시 예외 발생")
    void deletePost_타인게시글_예외() {
        // given
        CustomUserDetails anotherUser = new CustomUserDetails(2L, "other@test.com", "password");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.deletePost(1L, anotherUser))
                .isInstanceOf(CustomException.class);
    }
}
