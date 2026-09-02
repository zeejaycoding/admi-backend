package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.campus.CreateCampusRequest;
import com.powercity.power_city_platform.dto.request.campus.UpdateCampusRequest;
import com.powercity.power_city_platform.dto.request.campus.CampusSearchRequest;
import com.powercity.power_city_platform.dto.response.campus.CampusResponse;
import com.powercity.power_city_platform.dto.response.campus.CampusSummaryResponse;
import com.powercity.power_city_platform.dto.response.campus.CampusListResponse;
import com.powercity.power_city_platform.entity.Campus;
import com.powercity.power_city_platform.entity.ChildDedication;
import com.powercity.power_city_platform.entity.MarriageCertificate;
import com.powercity.power_city_platform.entity.TravelForm;
import com.powercity.power_city_platform.entity.Report;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.repository.CampusRepository;
import com.powercity.power_city_platform.repository.ChildDedicationRepository;
import com.powercity.power_city_platform.repository.MarriageCertificateRepository;
import com.powercity.power_city_platform.repository.TravelFormRepository;
import com.powercity.power_city_platform.repository.ReportRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import com.powercity.power_city_platform.util.RegionScope;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CampusService {

    private static final Logger logger = LoggerFactory.getLogger(CampusService.class);

    private final CampusRepository campusRepository;
    private final ChildDedicationRepository childDedicationRepository;
    private final MarriageCertificateRepository marriageCertificateRepository;
    private final TravelFormRepository travelFormRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getCampusStats() {
        String regionScope = RegionScope.resolve(userRepository);

        long total;
        long active;
        long featured;
        int totalRegions;

        if (regionScope != null) {
            // NATIONAL_LEADER: stats for their region only
            total = campusRepository.findByRegion(regionScope).size();
            active = campusRepository.findByRegionAndIsActive(regionScope, true).size();
            featured = campusRepository.findByIsFeaturedTrue().stream()
                    .filter(c -> regionScope.equalsIgnoreCase(c.getRegion()))
                    .count();
            totalRegions = 1;
        } else {
            total = campusRepository.count();
            active = campusRepository.countAllActiveCampuses();
            featured = campusRepository.countByIsFeaturedTrue();
            totalRegions = campusRepository.findDistinctRegions().size();
        }

        long inactive = total - active;
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCampuses", total);
        stats.put("activeCampuses", active);
        stats.put("inactiveCampuses", inactive);
        stats.put("featuredCampuses", featured);
        stats.put("totalRegions", totalRegions);
        return stats;
    }

    private final S3FileService s3FileService;

    // Per-campus real-data analytics (child dedications, marriages, travel forms, reports)
    @Transactional(readOnly = true)
    public Map<String, Object> getCampusAnalytics(Long campusId) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new IllegalArgumentException("Campus not found with id: " + campusId));
        String name = campus.getName();

        long childDedications = childDedicationRepository.countByCampus(name);
        long marriages = marriageCertificateRepository.countByCampus(name);
        long travelForms = travelFormRepository.countByCampus(name);
        long reports = reportRepository.countByCampus(name);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("childDedications", childDedications);
        totals.put("marriages", marriages);
        totals.put("travelForms", travelForms);
        totals.put("reports", reports);
        totals.put("totalRecords", childDedications + marriages + travelForms + reports);

        List<Map<String, Object>> registrationsByType = new ArrayList<>();
        registrationsByType.add(registrationTypeEntry("Child Dedications", childDedications, "#33CFFF"));
        registrationsByType.add(registrationTypeEntry("Marriages", marriages, "#40C4AA"));
        registrationsByType.add(registrationTypeEntry("Travel Forms", travelForms, "#EEEFF2"));
        registrationsByType.add(registrationTypeEntry("Reports", reports, "#FFD6A8"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("campusId", campusId);
        result.put("campusName", name);
        result.put("totals", totals);
        result.put("registrationsByType", registrationsByType);
        result.put("weeklyActivity", buildWeeklyActivity(name));
        result.put("monthlyActivity", buildMonthlyActivity(name));
        result.put("recentActivity", buildRecentActivity(name));
        result.put("generatedAt", LocalDateTime.now().toString());
        return result;
    }

    private Map<String, Object> registrationTypeEntry(String label, long count, String color) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", label);
        entry.put("count", count);
        entry.put("color", color);
        return entry;
    }

    private long countInRange(String campus, LocalDateTime start, LocalDateTime end) {
        return childDedicationRepository.countByCampusAndSubmittedAtBetween(campus, start, end)
                + marriageCertificateRepository.countByCampusAndSubmittedAtBetween(campus, start, end)
                + travelFormRepository.countByCampusAndSubmittedAtBetween(campus, start, end)
                + reportRepository.countByCampusAndCreatedAtBetween(campus, start, end);
    }

    private List<Map<String, Object>> buildWeeklyActivity(String campus) {
        List<Map<String, Object>> weekly = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            entry.put("count", countInRange(campus, start, end));
            weekly.add(entry);
        }
        return weekly;
    }

    private List<Map<String, Object>> buildMonthlyActivity(String campus) {
        List<Map<String, Object>> monthly = new ArrayList<>();
        LocalDate current = LocalDate.now().withDayOfMonth(1);
        for (int i = 11; i >= 0; i--) {
            LocalDate monthStart = current.minusMonths(i);
            LocalDateTime start = monthStart.atStartOfDay();
            LocalDateTime end = monthStart.plusMonths(1).atStartOfDay();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", monthStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            entry.put("count", countInRange(campus, start, end));
            monthly.add(entry);
        }
        return monthly;
    }

    private List<Map<String, Object>> buildRecentActivity(String campus) {
        List<Map<String, Object>> entries = new ArrayList<>();

        childDedicationRepository.findRecentByCampus(campus).stream().limit(3)
                .forEach(c -> entries.add(activityEntry(c.getId(), "Child Dedication",
                        "New child dedication submission", c.getSubmittedAt())));

        marriageCertificateRepository.findRecentByCampus(campus).stream().limit(3)
                .forEach(m -> entries.add(activityEntry(m.getId(), "Marriage",
                        "New marriage certificate submission", m.getSubmittedAt())));

        travelFormRepository.findRecentByCampus(campus).stream().limit(3)
                .forEach(t -> entries.add(activityEntry(t.getId(), "Travel Form",
                        "New travel form submission", t.getSubmittedAt())));

        reportRepository.findRecentByCampus(campus).stream().limit(3)
                .forEach(r -> entries.add(activityEntry(r.getId(), "Report",
                        "Financial report submitted for " + r.getCountry(),
                        r.getCreatedAt())));

        entries.sort(Comparator.comparing(
                (Map<String, Object> e) -> (LocalDateTime) e.get("date"), Comparator.reverseOrder()));

        return entries.stream().limit(8).collect(Collectors.toList());
    }

    private Map<String, Object> activityEntry(Long id, String type, String activity, LocalDateTime date) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("type", type);
        entry.put("activity", activity);
        entry.put("date", date);
        return entry;
    }

    // Create Campus
    @Transactional
    public CampusResponse createCampus(CreateCampusRequest request) {
        String regionScope = RegionScope.resolve(userRepository);

        // NATIONAL_LEADER can only create campuses in their own region
        if (regionScope != null) {
            request.setRegion(regionScope);
            request.setCountry(regionScope);
        }

        // Validate unique name per region
        if (campusRepository.existsByNameAndRegion(request.getName(), request.getRegion())) {
            throw new IllegalArgumentException("Campus with name '" + request.getName() + "' already exists in region '" + request.getRegion() + "'");
        }

        // Validate unique email if provided
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (campusRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Campus with email '" + request.getEmail() + "' already exists");
            }
        }

        Campus campus = new Campus();
        mapCreateRequestToEntity(request, campus);
        campus.setCreatedAt(LocalDateTime.now());
        campus.setUpdatedAt(LocalDateTime.now());

        reassignCoordinator(campus);

        Campus savedCampus = campusRepository.save(campus);

        return mapEntityToResponse(savedCampus);
    }

    // Update Campus
    @Transactional
    public CampusResponse updateCampus(Long campusId, UpdateCampusRequest request) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new IllegalArgumentException("Campus not found with id: " + campusId));

        String regionScope = RegionScope.resolve(userRepository);
        // NATIONAL_LEADER can only update campuses within their own region
        if (regionScope != null) {
            if (!regionScope.equals(campus.getRegion()) && campus.getRegion() != null) {
                throw new IllegalArgumentException("You can only manage campuses in your region");
            }
            request.setRegion(regionScope);
            request.setCountry(regionScope);
        }

        // Validate unique name per region if name or region is being updated
        if (request.getName() != null && request.getRegion() != null) {
            if (campusRepository.existsByNameAndRegionExcludingId(request.getName(), request.getRegion(), campusId)) {
                throw new IllegalArgumentException("Campus with name '" + request.getName() + "' already exists in region '" + request.getRegion() + "'");
            }
        }

        // Validate unique email if being updated
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (campusRepository.existsByEmailExcludingId(request.getEmail(), campusId)) {
                throw new IllegalArgumentException("Campus with email '" + request.getEmail() + "' already exists");
            }
        }

        mapUpdateRequestToEntity(request, campus);
        campus.setUpdatedAt(LocalDateTime.now());

        reassignCoordinator(campus);

        Campus updatedCampus = campusRepository.save(campus);

        return mapEntityToResponse(updatedCampus);
    }

    // Get Campus by ID
    @Transactional(readOnly = true)
    public CampusResponse getCampusById(Long campusId) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new IllegalArgumentException("Campus not found with id: " + campusId));
        return mapEntityToResponse(campus);
    }

    // Search Campuses
    @Transactional(readOnly = true)
    public CampusListResponse searchCampuses(CampusSearchRequest request) {
        // For NATIONAL_LEADER, force region filter to their assigned region
        String regionScope = RegionScope.resolve(userRepository);
        if (regionScope != null && (request.getRegion() == null || request.getRegion().isEmpty())) {
            request.setRegion(regionScope);
        }

        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Campus> campusPage = campusRepository.searchCampuses(
                request.getSearchTerm(),
                request.getRegion(),
                request.getCity(),
                request.getCountry(),
                request.getIsActive(),
                request.getIsFeatured(),
                pageable
        );

        List<CampusSummaryResponse> campuses = campusPage.getContent().stream()
                .map(this::mapEntityToSummaryResponse)
                .collect(Collectors.toList());

        return new CampusListResponse(
                campuses,
                campusPage.getTotalPages(),
                campusPage.getTotalElements(),
                campusPage.getNumber(),
                campusPage.getSize(),
                campusPage.hasNext(),
                campusPage.hasPrevious()
        );
    }

    // Get Campuses by Region
    @Transactional(readOnly = true)
    public List<CampusSummaryResponse> getCampusesByRegion(String region) {
        return campusRepository.findActiveCampusesByRegion(region).stream()
                .map(this::mapEntityToSummaryResponse)
                .collect(Collectors.toList());
    }

    // Get Featured Campuses
    @Transactional(readOnly = true)
    public List<CampusSummaryResponse> getFeaturedCampuses() {
        return campusRepository.findByIsFeaturedTrue().stream()
                .filter(campus -> campus.getIsActive())
                .map(this::mapEntityToSummaryResponse)
                .collect(Collectors.toList());
    }

    // Get Featured Campuses by Region
    @Transactional(readOnly = true)
    public List<CampusSummaryResponse> getFeaturedCampusesByRegion(String region) {
        List<Campus> campuses = campusRepository.findFeaturedCampusesByRegion(region);
        return campuses.stream()
                .map(this::mapEntityToSummaryResponse)
                .collect(Collectors.toList());
    }

    // Delete Campus
    public void deleteCampus(Long campusId) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new IllegalArgumentException("Campus not found with id: " + campusId));

        // Delete associated S3 files
        if (campus.getImageKey() != null) {
            s3FileService.deleteFile(campus.getImageKey());
        }

        campusRepository.delete(campus);
    }

    // Activate/Deactivate Campus
    public void updateCampusStatus(Long campusId, Boolean isActive) {
        campusRepository.updateCampusStatus(campusId, isActive);
    }

    // Update Featured Status
    public void updateFeaturedStatus(Long campusId, Boolean isFeatured) {
        campusRepository.updateFeaturedStatus(campusId, isFeatured);
    }

    // Analytics Methods
    // Note: Analytics data is now computed from actual member, event, and donation records
    // rather than aggregate counts stored on the Campus entity.
    // Implement analytics methods by querying the respective repositories (Member, Event, Donation)
    // when those entities are created.

    // Utility Methods
    @Transactional(readOnly = true)
    public List<String> getSupportedRegions() {
        return Arrays.asList("UK", "South Africa", "USA", "Ghana", "Nigeria");
    }

    @Transactional(readOnly = true)
    public List<Currency> getSupportedCurrencies() {
        return Arrays.asList(Currency.values());
    }

    @Transactional(readOnly = true)
    public Currency getCurrencyForRegion(String region) {
        return Currency.fromRegion(region);
    }

    // Private helper methods
    // Enforces one-campus-per-coordinator: when a coordinator email is assigned to a campus,
    // any other campus previously pointing at that email is released (set to null). This means
    // a coordinator moved to a new campus frees the old one.
    private void reassignCoordinator(Campus assignedCampus) {
        String email = assignedCampus.getCoordinatorEmail();
        if (email == null || email.isBlank()) {
            return;
        }
        List<Campus> candidates = campusRepository.findByCoordinatorEmailIgnoreCase(email);
        for (Campus candidate : candidates) {
            if (assignedCampus.getId() == null || !candidate.getId().equals(assignedCampus.getId())) {
                candidate.setCoordinatorEmail("");
            }
        }
    }

    private void mapCreateRequestToEntity(CreateCampusRequest request, Campus campus) {
        campus.setName(request.getName());
        campus.setRegion(request.getRegion());
        campus.setCurrency(request.getCurrency());
        campus.setDescription(request.getDescription());
        campus.setAddress(request.getAddress());
        campus.setCity(request.getCity());
        campus.setState(request.getState());
        campus.setCountry(request.getCountry());
        campus.setPhoneNumber(request.getPhoneNumber());
        campus.setEmail(request.getEmail());
        campus.setFacebookUrl(request.getFacebookUrl());
        campus.setInstagramUrl(request.getInstagramUrl());
        campus.setTwitterUrl(request.getTwitterUrl());
        campus.setCoordinator(request.getCoordinator());
        campus.setCoordinatorEmail(request.getCoordinatorEmail());
        campus.setIsFeatured(request.getIsFeatured());
        campus.setNotes(request.getNotes());
    }

    private void mapUpdateRequestToEntity(UpdateCampusRequest request, Campus campus) {
        if (request.getName() != null) campus.setName(request.getName());
        if (request.getRegion() != null) campus.setRegion(request.getRegion());
        if (request.getCurrency() != null) campus.setCurrency(request.getCurrency());
        if (request.getDescription() != null) campus.setDescription(request.getDescription());
        if (request.getAddress() != null) campus.setAddress(request.getAddress());
        if (request.getCity() != null) campus.setCity(request.getCity());
        if (request.getState() != null) campus.setState(request.getState());
        if (request.getCountry() != null) campus.setCountry(request.getCountry());
        if (request.getPhoneNumber() != null) campus.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) campus.setEmail(request.getEmail());
        if (request.getFacebookUrl() != null) campus.setFacebookUrl(request.getFacebookUrl());
        if (request.getInstagramUrl() != null) campus.setInstagramUrl(request.getInstagramUrl());
        if (request.getTwitterUrl() != null) campus.setTwitterUrl(request.getTwitterUrl());
        if (request.getCoordinator() != null) campus.setCoordinator(request.getCoordinator());
        if (request.getCoordinatorEmail() != null) campus.setCoordinatorEmail(request.getCoordinatorEmail());
        if (request.getIsActive() != null) campus.setIsActive(request.getIsActive());
        if (request.getIsFeatured() != null) campus.setIsFeatured(request.getIsFeatured());
        if (request.getNotes() != null) campus.setNotes(request.getNotes());
    }

    private CampusResponse mapEntityToResponse(Campus campus) {
        CampusResponse response = new CampusResponse();
        response.setId(campus.getId());
        response.setName(campus.getName());
        response.setRegion(campus.getRegion());
        response.setCurrency(campus.getCurrency());
        response.setDescription(campus.getDescription());
        response.setAddress(campus.getAddress());
        response.setCity(campus.getCity());
        response.setState(campus.getState());
        response.setCountry(campus.getCountry());
        response.setFullAddress(campus.getFullAddress());
        response.setPhoneNumber(campus.getPhoneNumber());
        response.setEmail(campus.getEmail());
        response.setFacebookUrl(campus.getFacebookUrl());
        response.setInstagramUrl(campus.getInstagramUrl());
        response.setTwitterUrl(campus.getTwitterUrl());
        response.setCoordinator(campus.getCoordinator());
        response.setCoordinatorEmail(campus.getCoordinatorEmail());
        response.setImageUrl(campus.getImageUrl());
        response.setIsActive(campus.getIsActive());
        response.setIsFeatured(campus.getIsFeatured());
        response.setNotes(campus.getNotes());
        response.setCreatedAt(campus.getCreatedAt());
        response.setUpdatedAt(campus.getUpdatedAt());
        return response;
    }

    private CampusSummaryResponse mapEntityToSummaryResponse(Campus campus) {
        CampusSummaryResponse response = new CampusSummaryResponse();
        response.setId(campus.getId());
        response.setName(campus.getName());
        response.setRegion(campus.getRegion());
        response.setCurrency(campus.getCurrency());
        response.setDescription(campus.getDescription());
        response.setCity(campus.getCity());
        response.setCountry(campus.getCountry());
        response.setFullAddress(campus.getFullAddress());
        response.setPhoneNumber(campus.getPhoneNumber());
        response.setEmail(campus.getEmail());
        response.setCoordinator(campus.getCoordinator());
        response.setCoordinatorEmail(campus.getCoordinatorEmail());
        response.setImageUrl(campus.getImageUrl());
        response.setIsActive(campus.getIsActive());
        response.setIsFeatured(campus.getIsFeatured());
        response.setCreatedAt(campus.getCreatedAt());
        return response;
    }


}
