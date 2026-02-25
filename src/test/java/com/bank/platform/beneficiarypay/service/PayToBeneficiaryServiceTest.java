package com.bank.platform.beneficiarypay.service;

import com.bank.platform.beneficiarypay.client.BeneficiaryClient;
import com.bank.platform.beneficiarypay.client.CustomerClient;
import com.bank.platform.beneficiarypay.client.PaymentsClient;
import com.bank.platform.beneficiarypay.dto.PayToBeneficiaryRequest;
import com.bank.platform.beneficiarypay.dto.PayToBeneficiaryResponse;
import com.bank.platform.common.error.ForbiddenBusinessException;
import com.bank.platform.common.validation.AmountValidator;
import com.bank.platform.common.validation.IfscValidator;
import com.bank.platform.persistence.IdempotencyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayToBeneficiaryServiceTest {

    @Mock
    private CustomerClient customerClient;
    @Mock
    private BeneficiaryClient beneficiaryClient;
    @Mock
    private PaymentsClient paymentsClient;

    private IdempotencyStore idempotencyStore;
    private IfscValidator ifscValidator;
    private AmountValidator amountValidator;
    private ObjectMapper objectMapper;

    private PayToBeneficiaryService service;

    @BeforeEach
    void setUp() {
        idempotencyStore = new IdempotencyStore();
        ifscValidator = new IfscValidator();
        amountValidator = new AmountValidator();
        objectMapper = new ObjectMapper();
        service = new PayToBeneficiaryService(
                customerClient, beneficiaryClient, paymentsClient,
                idempotencyStore, ifscValidator, amountValidator, objectMapper
        );
    }

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
    void happyPath_newBeneficiary_createsAndPays() {
        // given
        String customerId = "C001";
        PayToBeneficiaryRequest request = validRequest();

        when(customerClient.getCustomer(customerId))
                .thenReturn(new CustomerClient.CustomerInfo(customerId, "Test", "FULL"));
        when(beneficiaryClient.search(eq(customerId), eq("1234567890"), eq("HDFC0001234")))
                .thenReturn(null);
        when(beneficiaryClient.create(eq(customerId), any(BeneficiaryClient.CreateBeneficiaryRequest.class)))
                .thenReturn(new BeneficiaryClient.BeneficiaryInfo("BEN123", "Ravi Kumar", "1234567890", "HDFC0001234"));
        when(paymentsClient.pay(any(PaymentsClient.PaymentRequest.class)))
                .thenReturn(new PaymentsClient.PaymentResult("PAY987", "SUCCESS"));

        // when
        PayToBeneficiaryResponse response = service.execute(customerId, request);

        // then
        assertThat(response.beneficiaryId()).isEqualTo("BEN123");
        assertThat(response.paymentId()).isEqualTo("PAY987");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.correlationId()).isNotBlank();

        verify(customerClient).getCustomer(customerId);
        verify(beneficiaryClient).search(customerId, "1234567890", "HDFC0001234");
        verify(beneficiaryClient).create(eq(customerId), any());
        verify(paymentsClient).pay(any());
    }

    @Test
    void happyPath_existingBeneficiary_skipsCreate() {
        // given
        String customerId = "C001";
        PayToBeneficiaryRequest request = validRequest();

        when(customerClient.getCustomer(customerId))
                .thenReturn(new CustomerClient.CustomerInfo(customerId, "Test", "FULL"));
        when(beneficiaryClient.search(eq(customerId), eq("1234567890"), eq("HDFC0001234")))
                .thenReturn(new BeneficiaryClient.BeneficiaryInfo("BEN_EXISTING", "Ravi Kumar", "1234567890", "HDFC0001234"));
        when(paymentsClient.pay(any(PaymentsClient.PaymentRequest.class)))
                .thenReturn(new PaymentsClient.PaymentResult("PAY555", "SUCCESS"));

        // when
        PayToBeneficiaryResponse response = service.execute(customerId, request);

        // then
        assertThat(response.beneficiaryId()).isEqualTo("BEN_EXISTING");
        assertThat(response.paymentId()).isEqualTo("PAY555");

        verify(beneficiaryClient, never()).create(any(), any());
    }

    @Test
    void kycIncomplete_throwsForbidden() {
        // given
        String customerId = "C002";
        PayToBeneficiaryRequest request = validRequest();

        when(customerClient.getCustomer(customerId))
                .thenReturn(new CustomerClient.CustomerInfo(customerId, "Test", "PARTIAL"));

        // when / then
        assertThatThrownBy(() -> service.execute(customerId, request))
                .isInstanceOf(ForbiddenBusinessException.class)
                .hasMessageContaining("KYC")
                .satisfies(ex -> {
                    ForbiddenBusinessException fbe = (ForbiddenBusinessException) ex;
                    assertThat(fbe.getCode()).isEqualTo("KYC_INCOMPLETE");
                });

        verify(beneficiaryClient, never()).search(any(), any(), any());
        verify(paymentsClient, never()).pay(any());
    }

    @Test
    void invalidIfsc_throwsIllegalArgument() {
        // given
        String customerId = "C001";
        PayToBeneficiaryRequest request = new PayToBeneficiaryRequest(
                "Ravi Kumar", "1234567890", "BADIFSC",
                new BigDecimal("2500.00"), "INR", "Rent", "uuid-bad-ifsc"
        );

        // when / then
        assertThatThrownBy(() -> service.execute(customerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IFSC");
    }

    @Test
    void invalidAmount_throwsIllegalArgument() {
        // given
        String customerId = "C001";
        PayToBeneficiaryRequest request = new PayToBeneficiaryRequest(
                "Ravi Kumar", "1234567890", "HDFC0001234",
                new BigDecimal("-100.00"), "INR", "Rent", "uuid-bad-amount"
        );

        // when / then
        assertThatThrownBy(() -> service.execute(customerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount");
    }

    @Test
    void idempotentReplay_returnsCachedResponse() {
        // given
        String customerId = "C001";
        PayToBeneficiaryRequest request = validRequest();

        when(customerClient.getCustomer(customerId))
                .thenReturn(new CustomerClient.CustomerInfo(customerId, "Test", "FULL"));
        when(beneficiaryClient.search(eq(customerId), eq("1234567890"), eq("HDFC0001234")))
                .thenReturn(null);
        when(beneficiaryClient.create(eq(customerId), any()))
                .thenReturn(new BeneficiaryClient.BeneficiaryInfo("BEN123", "Ravi Kumar", "1234567890", "HDFC0001234"));
        when(paymentsClient.pay(any()))
                .thenReturn(new PaymentsClient.PaymentResult("PAY987", "SUCCESS"));

        // first call
        PayToBeneficiaryResponse first = service.execute(customerId, request);

        // reset mocks to verify second call does NOT hit downstream
        reset(customerClient, beneficiaryClient, paymentsClient);

        // second call with same requestId
        PayToBeneficiaryResponse second = service.execute(customerId, request);

        // then
        assertThat(second.beneficiaryId()).isEqualTo(first.beneficiaryId());
        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        assertThat(second.status()).isEqualTo(first.status());

        verifyNoInteractions(customerClient, beneficiaryClient, paymentsClient);
    }
}
