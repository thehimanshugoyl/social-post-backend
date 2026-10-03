package com.himanshu.social_post_backend.mapper;

import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import com.himanshu.social_post_backend.dto.request.UpdatePostRequest;
import com.himanshu.social_post_backend.dto.response.CommentResponse;
import com.himanshu.social_post_backend.dto.response.PostResponse;
import com.himanshu.social_post_backend.model.Comment;
import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.model.PostStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class PostMapper {

    public Post toEntity(CreatePostRequest request, String resolvedSlug) {
        PostStatus status = request.status() != null ? request.status() : PostStatus.DRAFT;
        Set<String> tags = request.tags() != null ? new HashSet<>(request.tags()) : new HashSet<>();

        return new Post(
                request.title().trim(),
                resolvedSlug,
                request.content().trim(),
                request.author().trim(),
                status,
                tags
        );
    }

    public PostResponse toResponse(Post post) {
        if (post == null) {
            return null;
        }

        int commentCount = post.getComments() != null ? post.getComments().size() : 0;

        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getContent(),
                post.getAuthor(),
                post.getStatus(),
                Set.copyOf(post.getTags()),
                commentCount,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public void updateEntityFromDto(UpdatePostRequest request, Post post) {
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        if (request.tags() != null) {
            post.setTags(new HashSet<>(request.tags()));
        }
    }

    public CommentResponse toCommentResponse(Comment comment) {
        if (comment == null) {
            return null;
        }

        Long postId = comment.getPost() != null ? comment.getPost().getId() : null;

        return new CommentResponse(
                comment.getId(),
                postId,
                comment.getAuthorName(),
                comment.getAuthorEmail(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
