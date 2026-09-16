package com.himanshu.social_post_backend.repository;

import com.himanshu.social_post_backend.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}