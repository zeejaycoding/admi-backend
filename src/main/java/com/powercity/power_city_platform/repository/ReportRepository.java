package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByCoordinator(String coordinator);
    List<Report> findByCreatedBy(Long createdBy);
}
