package com.himanshu.social_post_backend.service.impl;

import com.himanshu.social_post_backend.common.exception.ResourceNotFoundException;
import com.himanshu.social_post_backend.dto.request.CreateCommentRequest;
import com.himanshu.social_post_backend.dto.response.CommentResponse;
import com.himanshu.social_post_backend.mapper.PostMapper;
import com.himanshu.social_post_backend.model.Comment;
import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.repository.CommentRepository;
import com.himanshu.social_post_backend.repository.PostRepository;
import com.himanshu.social_post_backend.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository, PostMapper postMapper) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Override
    @Transactional
    public CommentResponse addComment(Long postId, CreateCommentRequest request) {
        log.info("Adding comment to post ID: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Comment comment = new Comment(
                request.authorName().trim(),
                request.authorEmail().trim().toLowerCase(),
                request.content().trim()
        );

        post.addComment(comment);
        Comment savedComment = commentRepository.save(comment);

        return postMapper.toCommentResponse(savedComment);
    }

    @Override
    public Page<CommentResponse> getCommentsByPostId(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }
        Page<Comment> commentPage = commentRepository.findByPostId(postId, pageable);
        return commentPage.map(postMapper::toCommentResponse);
    }

    @Override
    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        log.info("Deleting comment ID: {} from post ID: {}", commentId, postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getPost().getId().equals(postId)) {
            throw new ResourceNotFoundException("Comment", "postId", postId);
        }

        post.removeComment(comment);
        commentRepository.delete(comment);
    }
}
