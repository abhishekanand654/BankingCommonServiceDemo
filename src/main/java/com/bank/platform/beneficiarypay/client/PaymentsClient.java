package com.bank.platform.beneficiarypay.client;

import com.bank.platform.common.http.BankRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

@Component
public class PaymentsClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentsClient.class);

    private final BankRestClient restClient;
    private final String baseUrl;

    public PaymentsClient(BankRestClient restClient,
                          @Value("${bank.client.payments.baseUrl:http://localhost:8083}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public PaymentResult pay(PaymentRequest request) {
        log.info("Initiating payment for beneficiaryId={}, amount={}", request.beneficiaryId(), request.amount());
        URI uri = URI.create(baseUrl + "/v1/payments");
        return restClient.post(uri, request, PaymentResult.class, Map.of());
    }

    public record PaymentRequest(
            String customerId,
            String beneficiaryId,
            BigDecimal amount,
            String currency,
            String note,
            String requestId
    ) {}

    public record PaymentResult(String paymentId, String status) {}
}
