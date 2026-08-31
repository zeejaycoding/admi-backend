package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Optional<EmailLog> findByMessageId(String messageId);

    List<EmailLog> findByRecipient(String recipient);

    List<EmailLog> findByStatus(String status);

    @Query("SELECT e FROM EmailLog e WHERE e.sentAt BETWEEN :startDate AND :endDate")
    List<EmailLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.status = :status AND e.sentAt >= :date")
    long countByStatusAndDateAfter(@Param("status") String status, @Param("date") LocalDateTime date);
}
