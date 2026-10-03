package com.himanshu.social_post_backend.service.impl;

import com.himanshu.social_post_backend.common.exception.DuplicateResourceException;
import com.himanshu.social_post_backend.common.exception.ResourceNotFoundException;
import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import com.himanshu.social_post_backend.dto.request.UpdatePostRequest;
import com.himanshu.social_post_backend.dto.response.PostResponse;
import com.himanshu.social_post_backend.mapper.PostMapper;
import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.model.PostStatus;
import com.himanshu.social_post_backend.repository.PostRepository;
import com.himanshu.social_post_backend.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public PostServiceImpl(PostRepository postRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        log.info("Creating new post with title: '{}'", request.title());

        String slug = resolveSlug(request.slug(), request.title());
        if (postRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Post", "slug", slug);
        }

        Post post = postMapper.toEntity(request, slug);
        Post savedPost = postRepository.save(post);

        log.info("Post successfully created with ID: {} and slug: '{}'", savedPost.getId(), savedPost.getSlug());
        return postMapper.toResponse(savedPost);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "posts", key = "#id")
    public PostResponse getPostById(Long id) {
        log.info("Fetching post from database for ID: {}", id);
        Post post = findPostOrThrow(id);
        return postMapper.toResponse(post);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "post-slugs", key = "#slug")
    public PostResponse getPostBySlug(String slug) {
        log.info("Fetching post from database for slug: '{}'", slug);
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));
        return postMapper.toResponse(post);
    }

    @Override
    public Page<PostResponse> getPosts(PostStatus status, String keyword, Pageable pageable) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Page<Post> postPage = postRepository.findWithFilter(status, trimmedKeyword, pageable);
        return postPage.map(postMapper::toResponse);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"posts", "post-slugs", "author-stats"}, allEntries = true)
    public PostResponse updatePost(Long id, UpdatePostRequest request) {
        log.info("Updating post with ID: {}", id);
        Post post = findPostOrThrow(id);

        postMapper.updateEntityFromDto(request, post);
        Post updatedPost = postRepository.save(post);

        log.info("Post updated successfully with ID: {}", updatedPost.getId());
        return postMapper.toResponse(updatedPost);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"posts", "post-slugs", "author-stats"}, allEntries = true)
    public PostResponse updatePostStatus(Long id, PostStatus status) {
        log.info("Updating post status for ID: {} to {}", id, status);
        Post post = findPostOrThrow(id);
        post.setStatus(status);
        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"posts", "post-slugs", "author-stats"}, allEntries = true)
    public void deletePost(Long id) {
        log.info("Deleting post with ID: {}", id);
        Post post = findPostOrThrow(id);
        postRepository.delete(post);
        log.info("Post deleted successfully with ID: {}", id);
    }

    @Override
    public java.util.List<PostResponse> getAllPostsWithCommentsEager() {
        log.info("Executing JOIN FETCH query to prevent N+1 query problem");
        return postRepository.findAllWithCommentsEager().stream()
                .map(postMapper::toResponse)
                .toList();
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "author-stats")
    public java.util.List<com.himanshu.social_post_backend.dto.response.AuthorStatsProjection> getAuthorAnalytics() {
        log.info("Executing Native SQL query for author aggregation");
        return postRepository.getAuthorStatisticsNative();
    }

    private Post findPostOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
    }

    private String resolveSlug(String providedSlug, String title) {
        if (providedSlug != null && !providedSlug.trim().isEmpty()) {
            return providedSlug.trim().toLowerCase(Locale.ROOT);
        }
        return generateSlugFromTitle(title);
    }

    private String generateSlugFromTitle(String title) {
        String normalized = title.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9\\s-]", "");
        normalized = normalized.replaceAll("[\\s-]+", "-");
        if (normalized.endsWith("-")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
