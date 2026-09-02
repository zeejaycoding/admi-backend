package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.response.event.EventResponseDTO;
import com.powercity.power_city_platform.entity.Event;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.CourseCategory;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.EventRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    private boolean isAdminOrSuperAdmin(User user) {
        return user.getUserRoles().stream()
                .anyMatch(ur -> Boolean.TRUE.equals(ur.getIsActive()) && (
                        ur.getRole().getName().equals("ADMIN") ||
                        ur.getRole().getName().equals("SUPER_ADMIN")));
    }

    private void checkEventOwnership(Event event, User currentUser) {
        // The User passed from the controller is detached from the persistence
        // context (it was loaded in its own read-only transaction), so its lazy
        // userRoles/role collections cannot be read here without throwing a
        // LazyInitializationException. Re-fetch it inside this transaction so
        // the roles are initialized and ownership checks work reliably.
        User user = currentUser.getId() != null
                ? userRepository.findById(currentUser.getId()).orElse(currentUser)
                : currentUser;

        if (isAdminOrSuperAdmin(user)) return;
        if (!event.getCreatedBy().equals(user.getId())) {
            throw new RuntimeException("You can only modify events you created");
        }
    }

    // Helper to get creator User
    private User getCreator(Long creatorId) {
        if (creatorId == null) return null;
        return userRepository.findById(creatorId).orElse(null);
    }

    // Convert Event to EventResponseDTO
    public EventResponseDTO toResponseDTO(Event event) {
        return new EventResponseDTO(event, getCreator(event.getCreatedBy()));
    }

    // Create a new event
    public EventResponseDTO createEvent(String title, String description, CourseCategory module,
                                        LocalDate eventDate, String timeEstimate, LocalDate registrationDeadline,
                                        String location, String ticketType, String ticketCurrency, java.math.BigDecimal ticketPrice,
                                        MultipartFile thumbnailFile, User currentUser) {
        if (eventRepository.existsByTitle(title)) {
            throw new RuntimeException("Event with this title already exists");
        }

        try {
            Event event = new Event();
            event.setTitle(title);
            event.setDescription(description);
            event.setModule(module);
            event.setEventDate(eventDate);
            event.setTimeEstimate(timeEstimate);
            event.setRegistrationDeadline(registrationDeadline);
            if (location != null && !location.trim().isEmpty()) {
                event.setLocation(location);
            }
            event.setTicketType(ticketType != null ? ticketType : "FREE");
            if (ticketCurrency != null && !ticketCurrency.trim().isEmpty()) {
                try {
                    com.powercity.power_city_platform.enums.Currency.fromCode(ticketCurrency);
                    event.setTicketCurrency(ticketCurrency);
                } catch (IllegalArgumentException ignored) {
                    // fall back to default currency for invalid codes
                }
            }
            if (ticketPrice != null) event.setTicketPrice(ticketPrice);
            event.setIsActive(true);
            event.setCreatedBy(currentUser.getId());
            event.setUpdatedBy(currentUser.getId());

            // Save first to get ID
            Event savedEvent = eventRepository.save(event);

            // Upload banner to Cloudinary if provided
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                Map<String, String> uploadResult = cloudinaryService.uploadEventOrCampusImage(
                        thumbnailFile,
                        "events",
                        "event-" + savedEvent.getId()
                );
                savedEvent.setThumbnailUrl(uploadResult.get("url"));
                savedEvent.setThumbnailKey(uploadResult.get("publicId"));
                savedEvent = eventRepository.save(savedEvent);
            }

            return toResponseDTO(savedEvent);
        } catch (Exception e) {
            logger.error("Failed to create event. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create event: " + e.getMessage());
        }
    }

    // Update an existing event
    public EventResponseDTO updateEvent(Long id, String title, String description, CourseCategory module,
                                        LocalDate eventDate, String timeEstimate, LocalDate registrationDeadline,
                                        String location, Boolean isActive, String ticketType, String ticketCurrency, java.math.BigDecimal ticketPrice,
                                        MultipartFile thumbnailFile, User currentUser) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        checkEventOwnership(event, currentUser);

        if (title != null) {
            if (!title.equals(event.getTitle()) && eventRepository.existsByTitle(title)) {
                throw new RuntimeException("Event with this title already exists");
            }
            event.setTitle(title);
        }
        if (description != null) event.setDescription(description);
        if (module != null) event.setModule(module);
        if (eventDate != null) event.setEventDate(eventDate);
        if (timeEstimate != null) event.setTimeEstimate(timeEstimate);
        if (registrationDeadline != null) event.setRegistrationDeadline(registrationDeadline);
        if (location != null) event.setLocation(location);
        if (isActive != null) event.setIsActive(isActive);
        if (ticketType != null) event.setTicketType(ticketType);
        if (ticketCurrency != null && !ticketCurrency.trim().isEmpty()) {
            try {
                com.powercity.power_city_platform.enums.Currency.fromCode(ticketCurrency);
                event.setTicketCurrency(ticketCurrency);
            } catch (IllegalArgumentException ignored) {
                // fall back to existing currency for invalid codes
            }
        }
        if (ticketPrice != null) event.setTicketPrice(ticketPrice);

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            try {
                // Delete old banner if exists
                if (event.getThumbnailKey() != null) {
                    cloudinaryService.deleteFile(event.getThumbnailKey(), "image");
                }
                Map<String, String> uploadResult = cloudinaryService.uploadEventOrCampusImage(
                        thumbnailFile,
                        "events",
                        "event-" + id
                );
                event.setThumbnailUrl(uploadResult.get("url"));
                event.setThumbnailKey(uploadResult.get("publicId"));
            } catch (Exception e) {
                logger.error("Failed to upload event banner. Error: {}", e.getMessage());
            }
        }

        event.setUpdatedBy(currentUser.getId());
        event.setUpdatedAt(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);
        return toResponseDTO(savedEvent);
    }

    // Get event by ID
    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return toResponseDTO(event);
    }

    // Get all events
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getAllEvents(int page, int size, String sortBy, String sortDirection, Boolean includeInactive) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Event> eventsPage;
        if (Boolean.TRUE.equals(includeInactive)) {
            eventsPage = eventRepository.findAll(pageable);
        } else {
            eventsPage = eventRepository.findByIsActiveTrue(pageable);
        }

        List<EventResponseDTO> dtos = eventsPage.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, eventsPage.getTotalElements());
    }

    // Search events
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> searchEvents(String searchTerm, CourseCategory module, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Event> eventsPage;
        if (module != null) {
            eventsPage = eventRepository.searchEventsByModule(module, searchTerm, pageable);
        } else {
            eventsPage = eventRepository.searchEvents(searchTerm, pageable);
        }

        List<EventResponseDTO> dtos = eventsPage.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, eventsPage.getTotalElements());
    }

    // Delete event (hard delete)
    public void deleteEvent(Long id, User currentUser) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        checkEventOwnership(event, currentUser);

        // Best-effort cleanup of the Cloudinary thumbnail
        if (event.getThumbnailKey() != null && !event.getThumbnailKey().isEmpty()) {
            try {
                cloudinaryService.deleteFile(event.getThumbnailKey(), "image");
            } catch (Exception e) {
                logger.warn("Failed to delete event thumbnail from Cloudinary: {}", e.getMessage());
            }
        }

        eventRepository.delete(event);
    }

    // Get event analytics
    @Transactional(readOnly = true)
    public Map<String, Object> getEventAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        long totalEvents = eventRepository.count();
        long activeEvents = eventRepository.countActiveEvents();

        // Calculate upcoming vs completed based on event date
        LocalDate today = LocalDate.now();
        long upcomingEvents = eventRepository.findByIsActiveTrue().stream()
                .filter(e -> e.getEventDate() != null && e.getEventDate().isAfter(today.minusDays(1)))
                .count();

        long completedEvents = eventRepository.findByIsActiveTrue().stream()
                .filter(e -> e.getEventDate() != null && e.getEventDate().isBefore(today))
                .count();

        analytics.put("totalEvents", totalEvents);
        analytics.put("activeEvents", activeEvents);
        analytics.put("upcomingEvents", upcomingEvents);
        analytics.put("completedEvents", completedEvents);

        // Count by module
        Map<String, Long> moduleStats = new HashMap<>();
        for (CourseCategory cat : CourseCategory.values()) {
            moduleStats.put(cat.name(), eventRepository.countEventsByModule(cat));
        }
        analytics.put("eventsByModule", moduleStats);

        return analytics;
    }
}
