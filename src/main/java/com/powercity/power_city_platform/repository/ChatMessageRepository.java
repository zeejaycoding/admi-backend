package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<ChatMessage> findByConversationIdDesc(@Param("conversationId") Long conversationId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.readByRecipient = false " +
            "AND m.sender.id <> :userId")
    long countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(CASE WHEN m.readByRecipient = false AND m.sender.id <> :userId THEN 1 ELSE 0 END), 0) " +
            "FROM ChatMessage m WHERE m.conversation.id IN :conversationIds")
    long countTotalUnread(@Param("conversationIds") List<Long> conversationIds, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readByRecipient = true " +
            "WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND m.readByRecipient = false")
    void markConversationAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
