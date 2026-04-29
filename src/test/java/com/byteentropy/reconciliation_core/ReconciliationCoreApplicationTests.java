package com.byteentropy.reconciliation_core;

import com.byteentropy.reconciliation_core.client.BankInquiryClient;
import com.byteentropy.reconciliation_core.model.PaymentEntity;
import com.byteentropy.reconciliation_core.model.StatusInquiryResponse;
import com.byteentropy.reconciliation_core.repository.ReconciliationRepository;
import com.byteentropy.reconciliation_core.scheduler.ReconciliationWorker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class EventCoreApplicationTests {

    @Autowired
    private ReconciliationWorker worker;

    @Autowired
    private ReconciliationRepository repository;

    // Using the modern Spring Boot 3.4+ MockitoBean instead of deprecated MockBean
    @MockitoBean
    private BankInquiryClient bankClient;

    @BeforeEach
    void setup() {
        // Ensure a clean database state for every test run
        repository.deleteAll();
    }

    @Test
    void testReconciliationSuccess() {
        // 1. Arrange: Setup a transaction stuck in 'UNCERTAIN'
        // Backdated by 1 minute to ensure it passes the 30-second safety buffer
        String id = "txn-unique-123";
        PaymentEntity payment = new PaymentEntity();
        payment.setIdempotencyId(id);
        payment.setStatus("UNCERTAIN");
        payment.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        repository.save(payment);

        // 2. Mock: Instruct the mock bank client to return a successful resolution
        StatusInquiryResponse mockResponse = new StatusInquiryResponse(
            id, 
            "BANK-REF-777", 
            "AUTHORIZED", 
            "200", 
            "Transaction confirmed by bank"
        );
        when(bankClient.checkStatusAtBank(anyString())).thenReturn(mockResponse);

        // 3. Act: Run the reconciler
        worker.run();

        // 4. Assert: Confirm the record in the DB is now finalized
        Optional<PaymentEntity> updated = repository.findById(id);
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo("AUTHORIZED");
        assertThat(updated.get().getGatewayTxnId()).isEqualTo("BANK-REF-777");
        assertThat(updated.get().getMessage()).contains("Auto-resolved");
    }

    @Test
    void testSafetyBuffer_IgnoreRecentTransactions() {
        // 1. Arrange: Create a transaction that occurred only 5 seconds ago
        String id = "too-recent-to-fix";
        PaymentEntity payment = new PaymentEntity();
        payment.setIdempotencyId(id);
        payment.setStatus("UNCERTAIN");
        payment.setUpdatedAt(LocalDateTime.now().minusSeconds(5));
        repository.save(payment);

        // 2. Act: Trigger the worker
        worker.run();

        // 3. Assert: The status should remain UNCERTAIN (safety buffer working)
        PaymentEntity recordInDb = repository.findById(id).get();
        assertThat(recordInDb.getStatus()).isEqualTo("UNCERTAIN");
    }

    @Test
    void testBankStillUnsure_MaintainsUncertainStatus() {
        // 1. Arrange: An old transaction that is eligible for reconciliation
        String id = "bank-slow-id";
        PaymentEntity payment = new PaymentEntity();
        payment.setIdempotencyId(id);
        payment.setStatus("UNCERTAIN");
        payment.setUpdatedAt(LocalDateTime.now().minusMinutes(10));
        repository.save(payment);

        // 2. Mock: Bank responds with 'PENDING', meaning they aren't done yet
        StatusInquiryResponse pendingResponse = new StatusInquiryResponse(
            id, null, "PENDING", "102", "Request received, check back later"
        );
        when(bankClient.checkStatusAtBank(anyString())).thenReturn(pendingResponse);

        // 3. Act
        worker.run();

        // 4. Assert: Status should NOT change; it waits for the next cycle
        PaymentEntity recordInDb = repository.findById(id).get();
        assertThat(recordInDb.getStatus()).isEqualTo("UNCERTAIN");
    }
}
