package com.himanshu.social_post_backend.repository;

import com.himanshu.social_post_backend.model.DlqMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DlqMessageRepository extends JpaRepository<DlqMessage, Long> {

    Optional<DlqMessage> findByEventId(String eventId);

    List<DlqMessage> findByResolvedFalse();

    List<DlqMessage> findAllByOrderByFailureTimestampDesc();
}
