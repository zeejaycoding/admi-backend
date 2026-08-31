package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Drop the course link from any order lines so a course can be hard-deleted;
    // the order keeps its price/quantity record.
    @Modifying
    @Query(value = "UPDATE order_items SET course_id = NULL WHERE course_id = :courseId", nativeQuery = true)
    void detachCourse(@Param("courseId") Long courseId);
}
