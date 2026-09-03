package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.read = false")
    int markAllReadForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.userId = :userId")
    int markReadByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
