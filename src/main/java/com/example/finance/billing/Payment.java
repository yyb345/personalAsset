package com.example.finance.billing;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stripe_payments")
public class Payment {
    @Id public String id;
    @Column(nullable = false) public Long userId;
    public String sessionId;
    public String status;
    public Long amount;
    public String currency;
    public Instant createdAt;
    public String subscriptionId;
    public String customerId;
    public String subscriptionStatus;
    public Instant memberUntil;
    public Boolean cancelAtPeriodEnd = false;
    @Version public Long version;
    public Payment() {}
}
