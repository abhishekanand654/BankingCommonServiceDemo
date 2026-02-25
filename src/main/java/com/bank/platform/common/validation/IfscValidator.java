package com.bank.platform.common.validation;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class IfscValidator {

    // RBI IFSC format: 4 letters + 0 + 6 alphanumeric (commonly)
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    public void validate(String ifsc) {
        if (ifsc == null || ifsc.isBlank()) {
            throw new IllegalArgumentException("IFSC is required");
        }
        String normalized = ifsc.trim().toUpperCase();
        if (!IFSC.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid IFSC format");
        }
    }

    public String normalize(String ifsc) {
        return ifsc == null ? null : ifsc.trim().toUpperCase();
    }
}