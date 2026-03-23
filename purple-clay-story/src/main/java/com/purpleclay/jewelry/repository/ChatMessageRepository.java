package com.purpleclay.jewelry.repository;

import com.purpleclay.jewelry.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Last N messages for a session (for context window)
    @Query("SELECT c FROM ChatMessage c WHERE c.sessionId = :sessionId ORDER BY c.createdAt ASC")
    List<ChatMessage> findBySessionIdOrderByCreatedAt(@Param("sessionId") String sessionId);

    // All sessions for a user
    @Query("SELECT DISTINCT c.sessionId FROM ChatMessage c WHERE c.userId = :userId ORDER BY MAX(c.createdAt) DESC")
    List<String> findSessionIdsByUserId(@Param("userId") Long userId);

    // Delete old sessions to save DB space (admin cleanup)
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage c WHERE c.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);

    long countBySessionId(String sessionId);
}
