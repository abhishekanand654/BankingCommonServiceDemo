package com.bank.platform.common.web;

import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationId {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {}

    public static String getOrCreate() {
        String existing = MDC.get(MDC_KEY);
        if (existing != null && !existing.isBlank()) return existing;

        String created = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, created);
        return created;
    }

    public static void set(String id) {
        if (id != null && !id.isBlank()) {
            MDC.put(MDC_KEY, id);
        }
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}