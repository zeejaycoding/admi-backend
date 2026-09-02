package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByCoordinator(String coordinator);
    List<Report> findByCreatedBy(Long createdBy);

    @Query("SELECT COUNT(r) FROM Report r WHERE LOWER(r.campus) = LOWER(:campus)")
    long countByCampus(@Param("campus") String campus);

    @Query("SELECT COUNT(r) FROM Report r WHERE LOWER(r.campus) = LOWER(:campus) " +
           "AND r.createdAt >= :start AND r.createdAt < :end")
    long countByCampusAndCreatedAtBetween(@Param("campus") String campus,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Report r WHERE LOWER(r.campus) = LOWER(:campus) ORDER BY r.createdAt DESC")
    List<Report> findRecentByCampus(@Param("campus") String campus);
}
