package com.bank.platform.beneficiarypay.controller;

import com.bank.platform.beneficiarypay.dto.PayToBeneficiaryRequest;
import com.bank.platform.beneficiarypay.dto.PayToBeneficiaryResponse;
import com.bank.platform.beneficiarypay.service.PayToBeneficiaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayToBeneficiaryController.class)
class PayToBeneficiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayToBeneficiaryService service;

    @Autowired
    private ObjectMapper objectMapper;

    private PayToBeneficiaryRequest validRequest() {
        return new PayToBeneficiaryRequest(
                "Ravi Kumar",
                "1234567890",
                "HDFC0001234",
                new BigDecimal("2500.00"),
                "INR",
                "Rent",
                "uuid-123"
        );
    }

    @Test
    void happyPath_returns200() throws Exception {
        PayToBeneficiaryResponse response = new PayToBeneficiaryResponse(
                "BEN123", "PAY987", "SUCCESS", "corr-id-1"
        );
        when(service.execute(eq("C001"), any(PayToBeneficiaryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/customers/C001/beneficiaries/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beneficiaryId").value("BEN123"))
                .andExpect(jsonPath("$.paymentId").value("PAY987"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.correlationId").value("corr-id-1"));

        verify(service).execute(eq("C001"), any());
    }

    @Test
    void idempotentReplay_returns200WithSameResponse() throws Exception {
        PayToBeneficiaryResponse response = new PayToBeneficiaryResponse(
                "BEN123", "PAY987", "SUCCESS", "corr-id-1"
        );
        when(service.execute(eq("C001"), any(PayToBeneficiaryRequest.class)))
                .thenReturn(response);

        // First call
        mockMvc.perform(post("/v1/customers/C001/beneficiaries/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY987"));

        // Second call (idempotent replay - service handles idempotency internally)
        mockMvc.perform(post("/v1/customers/C001/beneficiaries/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY987"));

        verify(service, times(2)).execute(eq("C001"), any());
    }

    @Test
    void missingRequiredFields_returns400() throws Exception {
        // Empty request body with missing required fields
        String invalidJson = "{}";

        mockMvc.perform(post("/v1/customers/C001/beneficiaries/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void missingRequestId_returns400() throws Exception {
        String json = """
                {
                    "beneficiaryName": "Ravi Kumar",
                    "accountNumber": "1234567890",
                    "ifsc": "HDFC0001234",
                    "amount": 2500.00,
                    "currency": "INR",
                    "note": "Rent"
                }
                """;

        mockMvc.perform(post("/v1/customers/C001/beneficiaries/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
