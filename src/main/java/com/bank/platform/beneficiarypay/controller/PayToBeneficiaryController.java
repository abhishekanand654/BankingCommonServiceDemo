package com.bank.platform.beneficiarypay.controller;

import com.bank.platform.beneficiarypay.dto.PayToBeneficiaryRequest;
import com.bank.platform.beneficiarypay.dto.PayToBeneficiaryResponse;
import com.bank.platform.beneficiarypay.service.PayToBeneficiaryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/customers/{customerId}/beneficiaries")
public class PayToBeneficiaryController {

    private static final Logger log = LoggerFactory.getLogger(PayToBeneficiaryController.class);

    private final PayToBeneficiaryService service;

    public PayToBeneficiaryController(PayToBeneficiaryService service) {
        this.service = service;
    }

    @PostMapping("/pay")
    public ResponseEntity<PayToBeneficiaryResponse> payToNewBeneficiary(
            @PathVariable String customerId,
            @Valid @RequestBody PayToBeneficiaryRequest request
    ) {
        log.info("POST /v1/customers/{}/beneficiaries/pay requestId={}", customerId, request.requestId());
        PayToBeneficiaryResponse response = service.execute(customerId, request);
        return ResponseEntity.ok(response);
    }
}
