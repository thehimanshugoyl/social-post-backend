package com.himanshu.social_post_backend.repository;

import com.himanshu.social_post_backend.model.ProcessedEvent;
import com.himanshu.social_post_backend.model.ProcessedEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    Optional<ProcessedEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<ProcessedEvent> findByStatus(ProcessedEventStatus status);
}
