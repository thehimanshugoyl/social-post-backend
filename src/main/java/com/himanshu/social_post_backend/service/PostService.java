package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import com.himanshu.social_post_backend.dto.request.UpdatePostRequest;
import com.himanshu.social_post_backend.dto.response.PostResponse;
import com.himanshu.social_post_backend.model.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostResponse createPost(CreatePostRequest request);

    PostResponse getPostById(Long id);

    PostResponse getPostBySlug(String slug);

    Page<PostResponse> getPosts(PostStatus status, String keyword, Pageable pageable);

    PostResponse updatePost(Long id, UpdatePostRequest request);

    PostResponse updatePostStatus(Long id, PostStatus status);

    void deletePost(Long id);

    java.util.List<PostResponse> getAllPostsWithCommentsEager();

    java.util.List<com.himanshu.social_post_backend.dto.response.AuthorStatsProjection> getAuthorAnalytics();
}
