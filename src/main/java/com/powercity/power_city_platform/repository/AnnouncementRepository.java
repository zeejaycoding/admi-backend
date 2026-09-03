package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a ORDER BY a.createdAt DESC")
    List<Announcement> findAllOrderByCreatedAtDesc();

    @Query("SELECT a FROM Announcement a WHERE LOWER(a.region) = LOWER(:region) " +
            "ORDER BY a.createdAt DESC")
    List<Announcement> findByRegionOrderByCreatedAtDesc(@Param("region") String region);
}
