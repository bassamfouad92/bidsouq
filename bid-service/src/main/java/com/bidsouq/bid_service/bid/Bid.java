package com.bidsouq.bid_service.bid;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bids", indexes = {
        @Index(name = "idx_bid_auction_created", columnList = "auction_id, created_at"),
        @Index(name = "idx_bid_auction_amount",  columnList = "auction_id, amount")
})
@Getter
@NoArgsConstructor
public class Bid {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(nullable = false)
    private UUID auctionId;

    @Column(nullable = false)
    private UUID bidderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Bid(UUID auctionId, UUID bidderId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }
}
