package com.bank.platform.beneficiarypay.dto;

public record PayToBeneficiaryResponse(
        String beneficiaryId,
        String paymentId,
        String status,
        String correlationId
) {}
