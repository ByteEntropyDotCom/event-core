package com.byteentropy.event_core.scheduler;

import com.byteentropy.event_core.client.BankInquiryClient;
import com.byteentropy.event_core.model.PaymentEntity;
import com.byteentropy.event_core.model.StatusInquiryResponse;
import com.byteentropy.event_core.repository.ReconciliationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReconciliationWorker {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationWorker.class);

    private final ReconciliationRepository repository;
    private final BankInquiryClient bankClient;

    public ReconciliationWorker(ReconciliationRepository repository, BankInquiryClient bankClient) {
        this.repository = repository;
        this.bankClient = bankClient;
    }

    /**
     * The heart of the 99.99% ecosystem. 
     * Runs every 30 seconds to clean up "Uncertain" states.
     */
    @Scheduled(fixedDelayString = "${reconciliation.interval:30000}")
    @Transactional
    public void run() {
        // 1. Fetch only those stuck in UNCERTAIN status
        List<PaymentEntity> pendingTransactions = repository.findByStatus("UNCERTAIN");

        // 2. Filter for safety: Only reconcile if the record is older than 30 seconds
        // This prevents race conditions with the live 'resilience-core' processing.
        List<PaymentEntity> eligibleTransactions = pendingTransactions.stream()
            .filter(p -> p.getUpdatedAt() != null && 
                    p.getUpdatedAt().isBefore(LocalDateTime.now().minusSeconds(30)))
            .collect(Collectors.toList());

        if (eligibleTransactions.isEmpty()) {
            return;
        }

        log.info("[RECONCILER] Starting cleanup for {} eligible transactions", eligibleTransactions.size());

        for (PaymentEntity payment : eligibleTransactions) {
            reconcileSinglePayment(payment);
        }
    }

    private void reconcileSinglePayment(PaymentEntity payment) {
        String id = payment.getIdempotencyId();
        try {
            log.debug("[RECONCILER] Inquiring bank for ID: {}", id);
            
            // 3. Call the Bank API
            StatusInquiryResponse bankResponse = bankClient.checkStatusAtBank(id);

            // 4. Update based on structured response
            if (bankResponse.isResolved()) {
                payment.setStatus(bankResponse.status());
                payment.setGatewayTxnId(bankResponse.gatewayTxnId());
                payment.setMessage("Auto-resolved: " + bankResponse.description());
                payment.setUpdatedAt(LocalDateTime.now());

                repository.save(payment);
                log.info("[RECONCILER] SUCCESS: Transaction {} moved to {}", id, bankResponse.status());
            } else {
                log.warn("[RECONCILER] PENDING: Bank still unsure about ID: {}. Will retry next cycle.", id);
            }

        } catch (Exception e) {
            log.error("[RECONCILER] FAILED: Could not contact bank for ID: {}. Error: {}", id, e.getMessage());
            // We don't change the status here, so it will be picked up again in the next run.
        }
    }
}