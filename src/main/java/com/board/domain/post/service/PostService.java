package com.board.domain.post.service;

import com.board.domain.member.entity.Member;
import com.board.domain.member.repository.MemberRepository;
import com.board.domain.post.dto.PostCreateRequest;
import com.board.domain.post.dto.PostResponse;
import com.board.domain.post.dto.PostUpdateRequest;
import com.board.domain.post.entity.Post;
import com.board.domain.post.repository.PostRepository;
import com.board.global.exception.CustomException;
import com.board.global.exception.ErrorCode;
import com.board.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PostResponse createPost(CustomUserDetails userDetails, PostCreateRequest request) {
        Member member = memberRepository.findById(userDetails.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = Post.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return PostResponse.from(post);
    }

    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findAll(pageable).map(PostResponse::from);
    }

    public Page<PostResponse> searchPosts(String keyword, Pageable pageable) {
        return postRepository.findByTitleContaining(keyword, pageable).map(PostResponse::from);
    }

    @Transactional
    public PostResponse updatePost(Long postId, CustomUserDetails userDetails, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(userDetails.getId())) {
            throw new CustomException(ErrorCode.POST_AUTHOR_MISMATCH);
        }

        post.update(request.getTitle(), request.getContent());
        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId, CustomUserDetails userDetails) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(userDetails.getId())) {
            throw new CustomException(ErrorCode.POST_AUTHOR_MISMATCH);
        }

        postRepository.delete(post);
    }
}
