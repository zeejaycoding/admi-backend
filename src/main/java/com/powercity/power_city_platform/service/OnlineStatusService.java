package com.powercity.power_city_platform.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OnlineStatusService {

    private static final long ONLINE_WINDOW_MS = 60_000;

    private final Map<Long, Instant> lastSeen = new ConcurrentHashMap<>();

    public void markOnline(Long userId) {
        if (userId != null) {
            lastSeen.put(userId, Instant.now());
        }
    }

    public void markOffline(Long userId) {
        if (userId != null) {
            lastSeen.put(userId, Instant.now().minusSeconds(120));
        }
    }

    public boolean isOnline(Long userId) {
        Instant seen = lastSeen.get(userId);
        if (seen == null) {
            return false;
        }
        return seen.plusMillis(ONLINE_WINDOW_MS).isAfter(Instant.now());
    }

    public Set<Long> getOnlineUsers(Set<Long> userIds) {
        return userIds.stream()
                .filter(this::isOnline)
                .collect(Collectors.toSet());
    }

    public long countOnline() {
        long now = Instant.now().toEpochMilli();
        return lastSeen.entrySet().stream()
                .filter(e -> e.getValue().toEpochMilli() + ONLINE_WINDOW_MS > now)
                .count();
    }

    public void clear() {
        lastSeen.clear();
    }
}
