package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.ChatConversation;
import com.powercity.power_city_platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    @Query("SELECT c FROM ChatConversation c " +
            "WHERE (c.initiator.id = :userId OR c.participant.id = :userId) " +
            "ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC")
    List<ChatConversation> findAllForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatConversation c " +
            "WHERE c.isActive = true AND " +
            "((c.initiator.id = :userId AND c.participant.id = :otherId) " +
            " OR (c.initiator.id = :otherId AND c.participant.id = :userId))")
    Optional<ChatConversation> findByUsers(@Param("userId") Long userId, @Param("otherId") Long otherId);

    @Query("SELECT c FROM ChatConversation c " +
            "WHERE c.isActive = true AND c.initiator.id = :userId AND c.participant.id = :otherId")
    Optional<ChatConversation> findOneByUsers(@Param("userId") Long userId, @Param("otherId") Long otherId);

    @Query("SELECT COUNT(DISTINCT c.id) FROM ChatConversation c " +
            "WHERE c.initiator.id = :userId OR c.participant.id = :userId")
    long countConversationsForUser(@Param("userId") Long userId);
}
