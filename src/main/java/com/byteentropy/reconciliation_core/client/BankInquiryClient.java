package com.byteentropy.reconciliation_core.client;

import com.byteentropy.reconciliation_core.model.StatusInquiryResponse;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.Random;

@Component
public class BankInquiryClient {
    
    private final Random random = new Random();

    public StatusInquiryResponse checkStatusAtBank(String idempotencyId) {
        // Simulating the logic: 80% of the time the bank has finished processing
        boolean isResolved = random.nextDouble() < 0.8;
        
        if (isResolved) {
            // Randomly decide if it was a success or a failure at the bank
            String finalStatus = random.nextBoolean() ? "AUTHORIZED" : "FAILED";
            
            return new StatusInquiryResponse(
                idempotencyId,
                "GTW-" + UUID.randomUUID().toString().substring(0, 8),
                finalStatus,
                "200",
                "Bank successfully processed the transaction"
            );
        } else {
            // Bank is still working on it (e.g. pending manual review)
            return new StatusInquiryResponse(
                idempotencyId,
                null,
                "PENDING",
                "102",
                "Transaction still being processed at gateway"
            );
        }
    }
}
