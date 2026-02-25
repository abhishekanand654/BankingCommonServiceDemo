package com.bank.platform.beneficiarypay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayToBeneficiaryRequest(

        @NotBlank(message = "beneficiaryName is required")
        String beneficiaryName,

        @NotBlank(message = "accountNumber is required")
        String accountNumber,

        @NotBlank(message = "ifsc is required")
        String ifsc,

        @NotNull(message = "amount is required")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency,

        String note,

        @NotBlank(message = "requestId is required")
        String requestId
) {}
