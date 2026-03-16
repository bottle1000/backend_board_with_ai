package com.board.global.config;

import com.board.domain.comment.entity.Comment;
import com.board.domain.comment.repository.CommentRepository;
import com.board.domain.like.entity.PostLike;
import com.board.domain.like.repository.PostLikeRepository;
import com.board.domain.member.entity.Member;
import com.board.domain.member.repository.MemberRepository;
import com.board.domain.post.entity.Post;
import com.board.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MEMBER_COUNT   = 1_000;
    private static final int POST_COUNT     = 10_000;
    private static final int COMMENT_COUNT  = 50_000;
    private static final int LIKE_COUNT     = 30_000;

    @Override
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            log.info("더미 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("=== 더미 데이터 생성 시작 ===");
        long startTime = System.currentTimeMillis();

        List<Member> members = createMembers();
        List<Post>   posts   = createPosts(members);
        createComments(members, posts);
        createLikes(members, posts);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("=== 더미 데이터 생성 완료 (소요시간: {}ms) ===", elapsed);
    }

    private List<Member> createMembers() {
        log.info("회원 {}명 생성 중...", MEMBER_COUNT);

        // BCrypt는 느리므로 패스워드는 1번만 인코딩해서 재사용
        String encodedPassword = passwordEncoder.encode("password123");

        List<Member> members = new ArrayList<>();
        for (int i = 1; i <= MEMBER_COUNT; i++) {
            members.add(Member.builder()
                    .email("user" + i + "@test.com")
                    .password(encodedPassword)
                    .nickname("유저" + i)
                    .build());
        }

        List<Member> saved = memberRepository.saveAll(members);
        log.info("회원 생성 완료: {}명", saved.size());
        return saved;
    }

    private List<Post> createPosts(List<Member> members) {
        log.info("게시글 {}개 생성 중...", POST_COUNT);
        Random random = new Random();

        List<Post> posts = new ArrayList<>();
        for (int i = 1; i <= POST_COUNT; i++) {
            Member author = members.get(random.nextInt(members.size()));
            posts.add(Post.builder()
                    .member(author)
                    .title("게시글 제목 " + i)
                    .content("게시글 " + i + "번의 본문입니다. 성능 테스트를 위한 더미 데이터입니다. " +
                             "이 내용은 N+1 문제와 페이징 성능을 측정하기 위해 삽입되었습니다.")
                    .build());
        }

        List<Post> saved = postRepository.saveAll(posts);
        log.info("게시글 생성 완료: {}개", saved.size());
        return saved;
    }

    private void createComments(List<Member> members, List<Post> posts) {
        log.info("댓글 {}개 생성 중...", COMMENT_COUNT);
        Random random = new Random();

        List<Comment> comments = new ArrayList<>();
        for (int i = 1; i <= COMMENT_COUNT; i++) {
            Member author = members.get(random.nextInt(members.size()));
            Post   post   = posts.get(random.nextInt(posts.size()));

            comments.add(Comment.builder()
                    .post(post)
                    .member(author)
                    .content("댓글 내용 " + i + "번입니다.")
                    .build());

            // 1000개씩 나눠서 저장 (한 번에 50000개 올리면 OOM 날 수도 있어서)
            if (comments.size() == 1000) {
                commentRepository.saveAll(comments);
                comments.clear();
            }
        }

        // 남은 댓글 저장
        if (!comments.isEmpty()) {
            commentRepository.saveAll(comments);
        }

        log.info("댓글 생성 완료: {}개", COMMENT_COUNT);
    }

    private void createLikes(List<Member> members, List<Post> posts) {
        log.info("좋아요 {}개 생성 중...", LIKE_COUNT);
        Random random = new Random();

        // 중복 좋아요 방지용 (DB 조회 대신 메모리 셋 사용)
        Set<String> likeSet = new HashSet<>();
        List<PostLike> likes = new ArrayList<>();

        while (likes.size() < LIKE_COUNT) {
            Member member = members.get(random.nextInt(members.size()));
            Post   post   = posts.get(random.nextInt(posts.size()));

            String key = post.getId() + "_" + member.getId();
            if (likeSet.add(key)) {
                likes.add(PostLike.builder()
                        .post(post)
                        .member(member)
                        .build());
                post.increaseLikeCount();
            }
        }

        postLikeRepository.saveAll(likes);
        postRepository.saveAll(posts);  // likeCount 반영

        log.info("좋아요 생성 완료: {}개", likes.size());
    }
}
