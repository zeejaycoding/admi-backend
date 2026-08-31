package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Book;
import com.powercity.power_city_platform.enums.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByTitle(String title);
    boolean existsByTitle(String title);
    // Only active books reserve a title — a soft-deleted book shouldn't block re-creating it.
    boolean existsByTitleAndIsActiveTrue(String title);

    List<Book> findByIsActiveTrue();
    Page<Book> findByIsActiveTrue(Pageable pageable);

    List<Book> findByIsFeaturedTrueAndIsActiveTrue();
    Page<Book> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);

    List<Book> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title);
    Page<Book> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title, Pageable pageable);

    // Search by multiple criteria (title and description only)
    @Query("SELECT b FROM Book b WHERE b.isActive = true AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Book> searchBooks(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    List<Book> findByBasePriceBetweenAndIsActiveTrue(BigDecimal minPrice, BigDecimal maxPrice);
    List<Book> findByBasePriceLessThanEqualAndIsActiveTrue(BigDecimal maxPrice);
    List<Book> findByBasePriceGreaterThanEqualAndIsActiveTrue(BigDecimal minPrice);

    List<Book> findByBaseCurrencyAndIsActiveTrue(Currency currency);

    List<Book> findByLanguageAndIsActiveTrue(String language);

    List<Book> findByPublisherAndIsActiveTrue(String publisher);

    // Analytics queries
    @Query("SELECT b FROM Book b WHERE b.isActive = true ORDER BY b.salesCount DESC")
    List<Book> findTopSellingBooks(Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE b.isActive = true ORDER BY b.viewCount DESC")
    List<Book> findMostViewedBooks(Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE b.isActive = true ORDER BY b.downloadCount DESC")
    List<Book> findMostDownloadedBooks(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.isActive = true ORDER BY b.createdAt DESC")
    List<Book> findRecentBooks(Pageable pageable);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.isActive = true")
    Long countActiveBooks();

    @Query("SELECT COUNT(b) FROM Book b WHERE b.isActive = true AND b.isFeatured = true")
    Long countFeaturedBooks();

    // Price statistics
    @Query("SELECT MIN(b.basePrice) FROM Book b WHERE b.isActive = true")
    BigDecimal findMinPrice();
    
    @Query("SELECT MAX(b.basePrice) FROM Book b WHERE b.isActive = true")
    BigDecimal findMaxPrice();
    
    @Query("SELECT AVG(b.basePrice) FROM Book b WHERE b.isActive = true")
    BigDecimal findAveragePrice();

    List<Book> findByIdIn(List<Long> ids);
}
