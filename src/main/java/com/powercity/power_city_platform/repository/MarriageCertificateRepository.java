package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.MarriageCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarriageCertificateRepository extends JpaRepository<MarriageCertificate, Long> {

    List<MarriageCertificate> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<MarriageCertificate> findAllByOrderBySubmittedAtDesc();

    Optional<MarriageCertificate> findByCertificateNumber(String certificateNumber);

    @Query("SELECT COUNT(m) FROM MarriageCertificate m")
    long countTotal();

    long countByStatus(String status);

    @Query("SELECT m FROM MarriageCertificate m ORDER BY m.submittedAt DESC")
    List<MarriageCertificate> findRecent();

    @Query("SELECT COUNT(m) > 0 FROM MarriageCertificate m WHERE LOWER(m.groomName) = LOWER(:groomName) " +
           "AND LOWER(m.brideName) = LOWER(:brideName) AND m.marriageDate = :marriageDate")
    boolean existsDuplicate(@Param("groomName") String groomName,
                            @Param("brideName") String brideName,
                            @Param("marriageDate") LocalDate marriageDate);
}
