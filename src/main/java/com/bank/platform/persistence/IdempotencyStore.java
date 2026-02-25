package com.bank.platform.persistence;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency store for PoC.
 * In production, replace with Redis/DB (and add TTL cleanup).
 */
@Component
public class IdempotencyStore {

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public Optional<String> get(String requestId) {
        if (requestId == null || requestId.isBlank()) return Optional.empty();
        Entry entry = store.get(requestId);
        return entry == null ? Optional.empty() : Optional.of(entry.responseJson);
    }

    public void put(String requestId, String responseJson) {
        if (requestId == null || requestId.isBlank()) return;
        if (responseJson == null) responseJson = "";
        store.putIfAbsent(requestId, new Entry(responseJson, Instant.now()));
    }

    public record Entry(String responseJson, Instant createdAt) {}
}