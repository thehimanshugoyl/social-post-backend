package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.common.exception.DuplicateResourceException;
import com.himanshu.social_post_backend.common.exception.ResourceNotFoundException;
import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import com.himanshu.social_post_backend.dto.response.PostResponse;
import com.himanshu.social_post_backend.mapper.PostMapper;
import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.model.PostStatus;
import com.himanshu.social_post_backend.repository.PostRepository;
import com.himanshu.social_post_backend.service.impl.PostServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Exp 3.2.1: Unit Testing with JUnit 5 and Mockito.
 * Tests business logic of PostServiceImpl in complete isolation without Spring context overhead.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceUnitTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    @DisplayName("Unit Test: createPost successfully saves post and returns response DTO")
    void shouldCreatePostSuccessfully() {
        // Arrange
        CreatePostRequest request = new CreatePostRequest(
                "Unit Testing with JUnit 5",
                "unit-testing-junit-5",
                "Fast, isolated, and reliable test practices.",
                "himanshu",
                PostStatus.DRAFT,
                Set.of("testing", "junit5")
        );

        Post unmappedPost = new Post(
                request.title(),
                "unit-testing-junit-5",
                request.content(),
                request.author(),
                request.status(),
                request.tags()
        );

        Post savedPost = new Post(
                request.title(),
                "unit-testing-junit-5",
                request.content(),
                request.author(),
                request.status(),
                request.tags()
        );
        savedPost.setId(100L);

        PostResponse expectedResponse = new PostResponse(
                100L,
                request.title(),
                "unit-testing-junit-5",
                request.content(),
                request.author(),
                PostStatus.DRAFT,
                request.tags(),
                0,
                Instant.now(),
                Instant.now()
        );

        when(postRepository.existsBySlug("unit-testing-junit-5")).thenReturn(false);
        when(postMapper.toEntity(eq(request), eq("unit-testing-junit-5"))).thenReturn(unmappedPost);
        when(postRepository.save(unmappedPost)).thenReturn(savedPost);
        when(postMapper.toResponse(savedPost)).thenReturn(expectedResponse);

        // Act
        PostResponse result = postService.createPost(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.title()).isEqualTo("Unit Testing with JUnit 5");
        assertThat(result.status()).isEqualTo(PostStatus.DRAFT);

        verify(postRepository, times(1)).existsBySlug("unit-testing-junit-5");
        verify(postRepository, times(1)).save(unmappedPost);
    }

    @Test
    @DisplayName("Unit Test: createPost throws DuplicateResourceException when slug exists")
    void shouldThrowDuplicateExceptionWhenSlugExists() {
        CreatePostRequest request = new CreatePostRequest(
                "Duplicate Title",
                "duplicate-slug",
                "Content here with enough characters",
                "author",
                PostStatus.DRAFT,
                Set.of("tag")
        );

        when(postRepository.existsBySlug("duplicate-slug")).thenReturn(true);

        assertThatThrownBy(() -> postService.createPost(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("duplicate-slug");

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unit Test: getPostById throws ResourceNotFoundException when ID does not exist")
    void shouldThrowNotFoundExceptionWhenPostNotFound() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Unit Test: updatePostStatus transitions post state and saves")
    void shouldUpdatePostStatusSuccessfully() {
        Post post = new Post();
        post.setId(1L);
        post.setStatus(PostStatus.DRAFT);

        Post updatedPost = new Post();
        updatedPost.setId(1L);
        updatedPost.setStatus(PostStatus.PUBLISHED);

        PostResponse response = new PostResponse(
                1L, "Title", "slug", "Content", "author", PostStatus.PUBLISHED, Set.of(), 0, Instant.now(), Instant.now()
        );

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(updatedPost);
        when(postMapper.toResponse(updatedPost)).thenReturn(response);

        PostResponse result = postService.updatePostStatus(1L, PostStatus.PUBLISHED);

        assertThat(result.status()).isEqualTo(PostStatus.PUBLISHED);
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("Unit Test: deletePost deletes existing post")
    void shouldDeletePostSuccessfully() {
        Post post = new Post();
        post.setId(5L);

        when(postRepository.findById(5L)).thenReturn(Optional.of(post));
        doNothing().when(postRepository).delete(post);

        postService.deletePost(5L);

        verify(postRepository, times(1)).delete(post);
    }
}
