package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Campus;
import com.powercity.power_city_platform.enums.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long> {

       List<Campus> findByIsActiveTrue();

       List<Campus> findByIsFeaturedTrue();

       List<Campus> findByRegion(String region);

       List<Campus> findByRegionAndIsActive(String region, Boolean isActive);

       Optional<Campus> findByNameAndRegion(String name, String region);

       // Region-based queries
       @Query("SELECT c FROM Campus c WHERE c.region = :region AND c.isActive = true ORDER BY c.name ASC")
       List<Campus> findActiveCampusesByRegion(@Param("region") String region);

       @Query("SELECT c FROM Campus c WHERE c.region = :region AND c.isFeatured = true AND c.isActive = true ORDER BY c.name ASC")
       List<Campus> findFeaturedCampusesByRegion(@Param("region") String region);

       @Query("SELECT c FROM Campus c WHERE c.region = :region AND c.currency = :currency AND c.isActive = true")
       List<Campus> findCampusesByRegionAndCurrency(@Param("region") String region,
                     @Param("currency") Currency currency);

       // Search and filtering - native SQL for PostgreSQL compatibility
       @Query(value = "SELECT * FROM campuses c WHERE " +
                     "(:searchTerm IS NULL OR :searchTerm = '' OR " +
                     "c.name ILIKE '%' || :searchTerm || '%' OR " +
                     "c.description ILIKE '%' || :searchTerm || '%' OR " +
                     "c.city ILIKE '%' || :searchTerm || '%' OR " +
                     "c.address ILIKE '%' || :searchTerm || '%') AND " +
                     "(:region IS NULL OR c.region = :region) AND " +
                     "(:city IS NULL OR c.city ILIKE '%' || :city || '%') AND " +
                     "(:country IS NULL OR c.country ILIKE '%' || :country || '%') AND " +
                     "(:isActive IS NULL OR c.is_active = :isActive) AND " +
                     "(:isFeatured IS NULL OR c.is_featured = :isFeatured) " +
                     "ORDER BY c.name", nativeQuery = true)
       Page<Campus> searchCampuses(@Param("searchTerm") String searchTerm,
                     @Param("region") String region,
                     @Param("city") String city,
                     @Param("country") String country,
                     @Param("isActive") Boolean isActive,
                     @Param("isFeatured") Boolean isFeatured,
                     Pageable pageable);

       // Location-based queries
       @Query("SELECT c FROM Campus c WHERE c.city = :city AND c.isActive = true")
       List<Campus> findCampusesByCity(@Param("city") String city);

       @Query("SELECT c FROM Campus c WHERE c.country = :country AND c.isActive = true")
       List<Campus> findCampusesByCountry(@Param("country") String country);

       @Query("SELECT COUNT(c) FROM Campus c WHERE c.region = :region AND c.isActive = true")
       Long countActiveCampusesByRegion(@Param("region") String region);

       @Query("SELECT COUNT(c) FROM Campus c WHERE c.isActive = true")
       Long countAllActiveCampuses();

       Long countByIsFeaturedTrue();

       // Regional statistics
       @Query("SELECT c.region, COUNT(c) FROM Campus c WHERE c.isActive = true GROUP BY c.region ORDER BY COUNT(c) DESC")
       List<Object[]> getCampusCountByRegion();

       @Modifying
       @Query("UPDATE Campus c SET c.isActive = :isActive WHERE c.id = :campusId")
       void updateCampusStatus(@Param("campusId") Long campusId, @Param("isActive") Boolean isActive);

       @Modifying
       @Query("UPDATE Campus c SET c.isFeatured = :isFeatured WHERE c.id = :campusId")
       void updateFeaturedStatus(@Param("campusId") Long campusId, @Param("isFeatured") Boolean isFeatured);

       // Validation queries
       @Query("SELECT COUNT(c) > 0 FROM Campus c WHERE c.name = :name AND c.region = :region AND c.id != :excludeId")
       boolean existsByNameAndRegionExcludingId(@Param("name") String name, @Param("region") String region,
                     @Param("excludeId") Long excludeId);

       @Query("SELECT COUNT(c) > 0 FROM Campus c WHERE c.name = :name AND c.region = :region")
       boolean existsByNameAndRegion(@Param("name") String name, @Param("region") String region);

       @Query("SELECT COUNT(c) > 0 FROM Campus c WHERE c.email = :email AND c.id != :excludeId")
       boolean existsByEmailExcludingId(@Param("email") String email, @Param("excludeId") Long excludeId);

       @Query("SELECT COUNT(c) > 0 FROM Campus c WHERE c.email = :email")
       boolean existsByEmail(@Param("email") String email);

       // Distinct values for filtering
       @Query("SELECT DISTINCT c.region FROM Campus c WHERE c.isActive = true ORDER BY c.region")
       List<String> findDistinctRegions();

       @Query("SELECT DISTINCT c.city FROM Campus c WHERE c.isActive = true AND c.city IS NOT NULL ORDER BY c.city")
       List<String> findDistinctCities();

       @Query("SELECT DISTINCT c.country FROM Campus c WHERE c.isActive = true AND c.country IS NOT NULL ORDER BY c.country")
       List<String> findDistinctCountries();

       @Query("SELECT DISTINCT c.currency FROM Campus c WHERE c.isActive = true ORDER BY c.currency")
       List<Currency> findDistinctCurrencies();
}
