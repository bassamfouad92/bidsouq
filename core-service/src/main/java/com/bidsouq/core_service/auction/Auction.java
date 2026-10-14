package com.bidsouq.core_service.auction;

import com.bidsouq.core_service.car.Car;
import com.bidsouq.core_service.common.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auctions",
        indexes = {
                @Index(name = "idx_auction_status_end_time", columnList = "status, end_time"),
                @Index(name = "idx_auction_car", columnList = "car_id")
        }
)
@Setter
@Getter
@NoArgsConstructor
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal reservePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuctionStatus status = AuctionStatus.SCHEDULED;

    private UUID winnerId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Auction(Instant startTime, Instant endTime, BigDecimal startPrice, BigDecimal reservePrice, String description, Car car) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startPrice = startPrice;
        this.reservePrice = reservePrice;
        this.description = description;
        this.car = car;
    }
}
