package com.bank.platform.beneficiarypay.client;

import com.bank.platform.common.http.BankRestClient;
import com.bank.platform.common.http.DownstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;

@Component
public class BeneficiaryClient {

    private static final Logger log = LoggerFactory.getLogger(BeneficiaryClient.class);

    private final BankRestClient restClient;
    private final String baseUrl;

    public BeneficiaryClient(BankRestClient restClient,
                             @Value("${bank.client.beneficiary.baseUrl:http://localhost:8082}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    /**
     * Search for an existing beneficiary by customerId, accountNumber and ifsc.
     * Returns null if not found (downstream returns 404).
     */
    public BeneficiaryInfo search(String customerId, String accountNumber, String ifsc) {
        log.info("Searching beneficiary for customerId={}, accountNumber={}, ifsc={}", customerId, accountNumber, ifsc);
        URI uri = URI.create(baseUrl + "/v1/customers/" + customerId
                + "/beneficiaries/search?accountNumber=" + accountNumber + "&ifsc=" + ifsc);
        try {
            return restClient.get(uri, BeneficiaryInfo.class, Map.of());
        } catch (DownstreamException ex) {
            if (ex.getHttpStatus() == 404) {
                log.info("Beneficiary not found for customerId={}, accountNumber={}", customerId, accountNumber);
                return null;
            }
            throw ex;
        }
    }

    /**
     * Create a new beneficiary.
     */
    public BeneficiaryInfo create(String customerId, CreateBeneficiaryRequest request) {
        log.info("Creating beneficiary for customerId={}, name={}", customerId, request.beneficiaryName());
        URI uri = URI.create(baseUrl + "/v1/customers/" + customerId + "/beneficiaries");
        return restClient.post(uri, request, BeneficiaryInfo.class, Map.of());
    }

    public record BeneficiaryInfo(String beneficiaryId, String beneficiaryName, String accountNumber, String ifsc) {}

    public record CreateBeneficiaryRequest(String beneficiaryName, String accountNumber, String ifsc) {}
}
