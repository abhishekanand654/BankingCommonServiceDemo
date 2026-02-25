package com.bank.platform.beneficiarypay.client;

import com.bank.platform.common.http.BankRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;

@Component
public class CustomerClient {

    private static final Logger log = LoggerFactory.getLogger(CustomerClient.class);

    private final BankRestClient restClient;
    private final String baseUrl;

    public CustomerClient(BankRestClient restClient,
                          @Value("${bank.client.customer.baseUrl:http://localhost:8081}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public CustomerInfo getCustomer(String customerId) {
        log.info("Fetching customer info for customerId={}", customerId);
        URI uri = URI.create(baseUrl + "/v1/customers/" + customerId);
        return restClient.get(uri, CustomerInfo.class, Map.of());
    }

    public record CustomerInfo(String customerId, String name, String kycStatus) {}
}
