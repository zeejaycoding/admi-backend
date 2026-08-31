package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.ChatCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatCallRepository extends JpaRepository<ChatCall, Long> {

    @Query("SELECT c FROM ChatCall c WHERE c.conversation.id = :conversationId ORDER BY c.createdAt DESC")
    List<ChatCall> findByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT c FROM ChatCall c " +
            "WHERE c.initiator.id = :userId OR c.recipient.id = :userId " +
            "ORDER BY c.createdAt DESC")
    List<ChatCall> findAllForUser(@Param("userId") Long userId);

    Optional<ChatCall> findTopByInitiatorIdAndStatusInOrderByCreatedAtDesc(Long initiatorId, List<com.powercity.power_city_platform.enums.CallStatus> statuses);

    Optional<ChatCall> findTopByRecipientIdAndStatusInOrderByCreatedAtDesc(Long recipientId, List<com.powercity.power_city_platform.enums.CallStatus> statuses);
}
