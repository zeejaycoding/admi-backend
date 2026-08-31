package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.dto.response.event.EventResponseDTO;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.CourseCategory;
import com.powercity.power_city_platform.service.EventService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Management", description = "APIs for managing events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all events", description = "Get all events with pagination")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getAllEvents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "true") Boolean includeInactive) {
        Page<EventResponseDTO> events = eventService.getAllEvents(page, size, sortBy, sortDirection, includeInactive);
        return ResponseEntity.ok(ApiResponse.success("Events retrieved successfully", events));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Get detailed information about a specific event")
    public ResponseEntity<ApiResponse<EventResponseDTO>> getEventById(
            @Parameter(description = "Event ID", required = true) @PathVariable Long id) {
        EventResponseDTO event = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success("Event retrieved successfully", event));
    }

    @GetMapping("/search")
    @Operation(summary = "Search events", description = "Search events by title, description, or location")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> searchEvents(
            @RequestParam String searchTerm,
            @RequestParam(required = false) CourseCategory module,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        Page<EventResponseDTO> events = eventService.searchEvents(searchTerm, module, page, size);
        return ResponseEntity.ok(ApiResponse.success("Events search completed", events));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    @Operation(summary = "Create event", description = "Create a new event (Admins and National Leaders only)")
    public ResponseEntity<ApiResponse<EventResponseDTO>> createEvent(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam CourseCategory module,
            @RequestParam(required = false) LocalDate eventDate,
            @RequestParam(required = false) String timeEstimate,
            @RequestParam(required = false) LocalDate registrationDeadline,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String ticketType,
            @RequestParam(required = false) BigDecimal ticketPrice,
            @RequestParam(required = false) MultipartFile thumbnail) {
        User currentUser = userService.getCurrentUser();
        EventResponseDTO event = eventService.createEvent(
                title, description, module, eventDate,
                timeEstimate, registrationDeadline, location,
                ticketType, ticketPrice, thumbnail, currentUser
        );
        return ResponseEntity.ok(ApiResponse.success("Event created successfully", event));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    @Operation(summary = "Update event", description = "Update an existing event (Admins and National Leaders only)")
    public ResponseEntity<ApiResponse<EventResponseDTO>> updateEvent(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) CourseCategory module,
            @RequestParam(required = false) LocalDate eventDate,
            @RequestParam(required = false) String timeEstimate,
            @RequestParam(required = false) LocalDate registrationDeadline,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String ticketType,
            @RequestParam(required = false) BigDecimal ticketPrice,
            @RequestParam(required = false) MultipartFile thumbnail) {
        User currentUser = userService.getCurrentUser();
        EventResponseDTO event = eventService.updateEvent(
                id, title, description, module, eventDate,
                timeEstimate, registrationDeadline, location,
                isActive, ticketType, ticketPrice, thumbnail, currentUser
        );
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", event));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    @Operation(summary = "Delete event", description = "Soft delete an event (Admins and National Leaders only)")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        eventService.deleteEvent(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully", null));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('NATIONAL_LEADER')")
    @Operation(summary = "Get event analytics", description = "Get event statistics (Admins and National Leaders only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventAnalytics() {
        Map<String, Object> analytics = eventService.getEventAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }
}
