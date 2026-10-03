package com.himanshu.social_post_backend.repository;

import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.model.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Post> findBySlug(String slug);

    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> findWithFilter(
            @Param("status") PostStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * Resolves the N+1 query problem by executing a single JOIN FETCH query
     * that eagerly loads posts along with their associated comments in one trip.
     */
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.comments ORDER BY p.createdAt DESC")
    java.util.List<Post> findAllWithCommentsEager();

    /**
     * Native SQL query performing complex relational aggregation across tables.
     */
    @Query(value = "SELECT p.author AS author, " +
                   "COUNT(DISTINCT p.id) AS totalPosts, " +
                   "COUNT(c.id) AS totalComments " +
                   "FROM posts p " +
                   "LEFT JOIN comments c ON p.id = c.post_id " +
                   "GROUP BY p.author " +
                   "ORDER BY totalPosts DESC", nativeQuery = true)
    java.util.List<com.himanshu.social_post_backend.dto.response.AuthorStatsProjection> getAuthorStatisticsNative();
}
