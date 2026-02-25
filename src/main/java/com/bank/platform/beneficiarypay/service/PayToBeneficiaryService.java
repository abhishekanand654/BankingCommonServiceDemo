package com.bank.platform.beneficiarypay.service;

import com.bank.platform.beneficiarypay.client.BeneficiaryClient;
import com.bank.platform.beneficiarypay.client.CustomerClient;
import com.bank.platform.beneficiarypay.client.PaymentsClient;
import com.bank.platform.beneficiarypay.dto.PayToBeneficiaryRequest;
import com.bank.platform.beneficiarypay.dto.PayToBeneficiaryResponse;
import com.bank.platform.common.error.ForbiddenBusinessException;
import com.bank.platform.common.validation.AmountValidator;
import com.bank.platform.common.validation.IfscValidator;
import com.bank.platform.common.web.CorrelationId;
import com.bank.platform.persistence.IdempotencyStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PayToBeneficiaryService {

    private static final Logger log = LoggerFactory.getLogger(PayToBeneficiaryService.class);

    private final CustomerClient customerClient;
    private final BeneficiaryClient beneficiaryClient;
    private final PaymentsClient paymentsClient;
    private final IdempotencyStore idempotencyStore;
    private final IfscValidator ifscValidator;
    private final AmountValidator amountValidator;
    private final ObjectMapper objectMapper;

    public PayToBeneficiaryService(CustomerClient customerClient,
                                   BeneficiaryClient beneficiaryClient,
                                   PaymentsClient paymentsClient,
                                   IdempotencyStore idempotencyStore,
                                   IfscValidator ifscValidator,
                                   AmountValidator amountValidator,
                                   ObjectMapper objectMapper) {
        this.customerClient = customerClient;
        this.beneficiaryClient = beneficiaryClient;
        this.paymentsClient = paymentsClient;
        this.idempotencyStore = idempotencyStore;
        this.ifscValidator = ifscValidator;
        this.amountValidator = amountValidator;
        this.objectMapper = objectMapper;
    }

    public PayToBeneficiaryResponse execute(String customerId, PayToBeneficiaryRequest request) {
        String correlationId = CorrelationId.getOrCreate();
        String requestId = request.requestId();

        log.info("PayToBeneficiary started: customerId={}, requestId={}, correlationId={}",
                customerId, requestId, correlationId);

        // 1. Idempotency check
        Optional<String> cached = idempotencyStore.get(requestId);
        if (cached.isPresent()) {
            log.info("Idempotent replay for requestId={}, correlationId={}", requestId, correlationId);
            return deserialize(cached.get());
        }

        // 2. Validate IFSC and amount
        ifscValidator.validate(request.ifsc());
        amountValidator.validate(request.amount());

        // 3. Fetch customer and check KYC
        log.info("Fetching customer: customerId={}, correlationId={}", customerId, correlationId);
        CustomerClient.CustomerInfo customer = customerClient.getCustomer(customerId);
        if (!"FULL".equalsIgnoreCase(customer.kycStatus())) {
            log.warn("KYC incomplete for customerId={}, kycStatus={}, correlationId={}",
                    customerId, customer.kycStatus(), correlationId);
            throw new ForbiddenBusinessException("KYC_INCOMPLETE",
                    "Customer KYC status is not FULL. Current status: " + customer.kycStatus());
        }

        // 4. Search for existing beneficiary
        String normalizedIfsc = ifscValidator.normalize(request.ifsc());
        log.info("Searching beneficiary: customerId={}, accountNumber={}, correlationId={}",
                customerId, request.accountNumber(), correlationId);
        BeneficiaryClient.BeneficiaryInfo beneficiary =
                beneficiaryClient.search(customerId, request.accountNumber(), normalizedIfsc);

        // 5. Create beneficiary if not found
        if (beneficiary == null) {
            log.info("Beneficiary not found, creating: customerId={}, correlationId={}", customerId, correlationId);
            BeneficiaryClient.CreateBeneficiaryRequest createReq =
                    new BeneficiaryClient.CreateBeneficiaryRequest(
                            request.beneficiaryName(),
                            request.accountNumber(),
                            normalizedIfsc
                    );
            beneficiary = beneficiaryClient.create(customerId, createReq);
            log.info("Beneficiary created: beneficiaryId={}, correlationId={}",
                    beneficiary.beneficiaryId(), correlationId);
        } else {
            log.info("Beneficiary already exists: beneficiaryId={}, correlationId={}",
                    beneficiary.beneficiaryId(), correlationId);
        }

        // 6. Execute payment
        log.info("Executing payment: beneficiaryId={}, amount={}, correlationId={}",
                beneficiary.beneficiaryId(), request.amount(), correlationId);
        PaymentsClient.PaymentRequest paymentReq = new PaymentsClient.PaymentRequest(
                customerId,
                beneficiary.beneficiaryId(),
                request.amount(),
                request.currency(),
                request.note(),
                requestId
        );
        PaymentsClient.PaymentResult paymentResult = paymentsClient.pay(paymentReq);

        // 7. Build response
        PayToBeneficiaryResponse response = new PayToBeneficiaryResponse(
                beneficiary.beneficiaryId(),
                paymentResult.paymentId(),
                paymentResult.status(),
                correlationId
        );

        // 8. Store for idempotency
        idempotencyStore.put(requestId, serialize(response));
        log.info("PayToBeneficiary completed: paymentId={}, requestId={}, correlationId={}",
                paymentResult.paymentId(), requestId, correlationId);

        return response;
    }

    private String serialize(PayToBeneficiaryResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response", e);
            throw new RuntimeException("Serialization error", e);
        }
    }

    private PayToBeneficiaryResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, PayToBeneficiaryResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached response", e);
            throw new RuntimeException("Deserialization error", e);
        }
    }
}
