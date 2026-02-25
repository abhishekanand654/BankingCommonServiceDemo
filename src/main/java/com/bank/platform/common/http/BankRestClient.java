
package com.bank.platform.common.http;

import com.bank.platform.common.security.ServiceAuthTokenProvider;
import com.bank.platform.common.web.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Component
public class BankRestClient {

    private static final Logger log = LoggerFactory.getLogger(BankRestClient.class);

    private final RestTemplate restTemplate;
    private final ServiceAuthTokenProvider tokenProvider;

    public BankRestClient(RestTemplate restTemplate, ServiceAuthTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
    }

    public <T> T get(URI uri, Class<T> responseType, Map<String, String> extraHeaders) {
        return exchange(uri, HttpMethod.GET, null, responseType, extraHeaders);
    }

    public <T, R> R post(URI uri, T body, Class<R> responseType, Map<String, String> extraHeaders) {
        return exchange(uri, HttpMethod.POST, body, responseType, extraHeaders);
    }

    public <T, R> R put(URI uri, T body, Class<R> responseType, Map<String, String> extraHeaders) {
        return exchange(uri, HttpMethod.PUT, body, responseType, extraHeaders);
    }

    public void delete(URI uri, Map<String, String> extraHeaders) {
        exchange(uri, HttpMethod.DELETE, null, Void.class, extraHeaders);
    }

    private <T, R> R exchange(
            URI uri,
            HttpMethod method,
            T body,
            Class<R> responseType,
            Map<String, String> extraHeaders
    ) {
        Objects.requireNonNull(uri, "uri must not be null");
        Objects.requireNonNull(method, "method must not be null");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Correlation ID
        headers.set(CorrelationId.HEADER_NAME, CorrelationId.getOrCreate());

        // Service-to-service auth (stub token provider for PoC)
        String token = tokenProvider.getServiceToken();
        if (token != null && !token.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        if (extraHeaders != null) {
            extraHeaders.forEach(headers::set);
        }

        HttpEntity<T> entity = new HttpEntity<>(body, headers);

        try {
            log.debug("HTTP {} {}", method, uri);
            ResponseEntity<R> response = restTemplate.exchange(uri, method, entity, responseType);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DownstreamException("DOWNSTREAM_NON_2XX",
                        "Downstream returned status: " + response.getStatusCode(),
                        response.getStatusCodeValue(),
                        uri.toString());
            }
            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            // Downstream returned HTTP status with body
            int status = ex.getStatusCode().value();
            String bodyStr = ex.getResponseBodyAsString();
            throw new DownstreamException("DOWNSTREAM_HTTP_ERROR",
                    "Downstream error: " + status,
                    status,
                    uri.toString(),
                    bodyStr);

        } catch (ResourceAccessException ex) {
            // Timeouts / connection issues
            throw new DownstreamException("DOWNSTREAM_TIMEOUT_OR_NETWORK",
                    "Downstream connectivity issue: " + ex.getMessage(),
                    504,
                    uri.toString());

        } catch (RestClientException ex) {
            throw new DownstreamException("DOWNSTREAM_CLIENT_ERROR",
                    "Downstream client error: " + ex.getMessage(),
                    502,
                    uri.toString());
        }
    }

    /**
     * Configuration: create a RestTemplate bean with sensible defaults.
     */
    @Component
    public static class RestTemplateFactory {

        public RestTemplate restTemplate() {
            // You can inject RestTemplateBuilder if you prefer.
            RestTemplate template = new RestTemplate();
            template.setErrorHandler(new NoOpErrorHandler()); // handle errors ourselves
            // For production: use HttpClient with timeouts; keeping simple for PoC.
            return template;
        }
    }

    private static class NoOpErrorHandler implements ResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return false; // never throw, we handle based on status ourselves
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            // no-op
        }
    }
}