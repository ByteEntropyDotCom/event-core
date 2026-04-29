package com.byteentropy.reconciliation_core.model;

/**
 * Structured response from the Bank's Status Inquiry API.
 */
public record StatusInquiryResponse(
    String idempotencyId,
    String gatewayTxnId,
    String status,         // AUTHORIZED, FAILED, or DECLINED
    String bankResponseCode,
    String description
) {
    // Helper method to check if the status is final
    public boolean isResolved() {
        return "AUTHORIZED".equals(status) || "FAILED".equals(status) || "DECLINED".equals(status);
    }
}
