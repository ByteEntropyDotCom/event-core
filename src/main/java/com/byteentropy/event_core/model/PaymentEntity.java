package com.byteentropy.event_core.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
public class PaymentEntity {
    @Id
    private String idempotencyId;
    private String status;
    private String gatewayTxnId;
    private String message;
    private LocalDateTime updatedAt;

    public PaymentEntity() {}

    // Add this Getter (This is what the worker was looking for)
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Existing Getters and Setters
    public String getIdempotencyId() { return idempotencyId; }
    public void setIdempotencyId(String id) { this.idempotencyId = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGatewayTxnId() { return gatewayTxnId; }
    public void setGatewayTxnId(String txnId) { this.gatewayTxnId = txnId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}