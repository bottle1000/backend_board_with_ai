package com.board.domain.like.service;

import com.board.domain.like.repository.PostLikeRepository;
import com.board.domain.member.entity.Member;
import com.board.domain.member.repository.MemberRepository;
import com.board.domain.post.entity.Post;
import com.board.domain.post.repository.PostRepository;
import com.board.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PostLikeServiceConcurrencyTest {

    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    private Post testPost;
    private List<Member> testMembers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 테스트용 멤버 50명 생성
        for (int i = 1; i <= 50; i++) {
            Member member = memberRepository.save(Member.builder()
                    .email("concurrent" + i + "@test.com")
                    .password("password123")
                    .nickname("동시성테스트유저" + i)
                    .build());
            testMembers.add(member);
        }

        // 테스트용 게시글 1개 생성 (likeCount = 0)
        testPost = postRepository.save(Post.builder()
                .member(testMembers.get(0))
                .title("동시성 테스트 게시글")
                .content("이 게시글로 동시성 테스트를 진행합니다.")
                .build());
    }

    @AfterEach
    void tearDown() {
        postLikeRepository.deleteAll();
        postRepository.delete(testPost);
        memberRepository.deleteAll(testMembers);
        testMembers.clear();
    }

    @Test
    @DisplayName("50명이 동시에 좋아요를 누르면 likeCount가 50이어야 한다 - 동시성 문제 확인")
    void 동시에_좋아요를_누르면_likeCount가_정확해야_한다() throws InterruptedException {
        // given
        int threadCount = 50;

        // 스레드 풀: 50개 스레드 동시 실행
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // CountDownLatch: 모든 스레드가 완료될 때까지 메인 스레드 대기
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 50명이 동시에 같은 게시글에 좋아요
        for (int i = 0; i < threadCount; i++) {
            final Member member = testMembers.get(i);

            executorService.submit(() -> {
                try {
                    CustomUserDetails userDetails = new CustomUserDetails(
                            member.getId(),
                            member.getEmail(),
                            member.getPassword()
                    );
                    postLikeService.toggleLike(testPost.getId(), userDetails);
                } finally {
                    latch.countDown(); // 완료된 스레드 카운트 차감
                }
            });
        }

        latch.await();        // 50개 스레드 모두 완료까지 대기
        executorService.shutdown();

        // then: 50명이 좋아요 눌렀으니 likeCount = 50 이어야 함
        Post result = postRepository.findById(testPost.getId()).orElseThrow();

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("기대값: " + threadCount);
        System.out.println("실제값: " + result.getLikeCount());
        System.out.println("차이: " + (threadCount - result.getLikeCount()) + "개 유실");

        // 동시성 문제가 있으면 이 테스트는 실패함
        assertThat(result.getLikeCount()).isEqualTo(threadCount);
    }
}
