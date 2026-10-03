package com.himanshu.social_post_backend.dto.response;

/**
 * Spring Data JPA projection interface for Native SQL aggregation results.
 */
public interface AuthorStatsProjection {
    String getAuthor();
    long getTotalPosts();
    long getTotalComments();
}
