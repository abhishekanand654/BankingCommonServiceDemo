package com.bank.platform.common.validation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmountValidator {

    public void validate(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount is required");
        if (amount.scale() > 2) throw new IllegalArgumentException("Amount can have max 2 decimals");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be > 0");
    }

    public void validate(BigDecimal amount, BigDecimal minInclusive, BigDecimal maxInclusive) {
        validate(amount);
        if (minInclusive != null && amount.compareTo(minInclusive) < 0) {
            throw new IllegalArgumentException("Amount must be >= " + minInclusive);
        }
        if (maxInclusive != null && amount.compareTo(maxInclusive) > 0) {
            throw new IllegalArgumentException("Amount must be <= " + maxInclusive);
        }
    }
}