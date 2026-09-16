package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.dto.PostRequest;
import com.himanshu.social_post_backend.dto.PostResponse;
import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.repository.PostRepository;
import com.himanshu.social_post_backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public PostResponse createPost(PostRequest request) {
        Post post = new Post();
        post.setText(request.getText());
        post.setPlatformIds(request.getPlatformIds());
        post.setStatus("draft");

        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        return toResponse(post);
    }

    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        post.setText(request.getText());
        post.setPlatformIds(request.getPlatformIds());

        Post updated = postRepository.save(post);
        return toResponse(updated);
    }

    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new ResourceNotFoundException("Post not found with id: " + id);
        }
        postRepository.deleteById(id);
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getText(),
                post.getPlatformIds(),
                post.getStatus(),
                post.getScheduledAt(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}