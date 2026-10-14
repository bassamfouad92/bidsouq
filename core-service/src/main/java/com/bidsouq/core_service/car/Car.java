package com.bidsouq.core_service.car;

import com.bidsouq.core_service.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cars")
@Setter
@Getter
@NoArgsConstructor
public class Car {
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Column(nullable = false, length = 100)
   private String make;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "model_year", nullable = false)
    private Integer year;

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "car", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarAttachment> attachments = new ArrayList<>();

    public Car(String vin, String make, String model, Integer year, User seller) {
        this.vin = vin;
        this.make = make;
        this.model = model;
        this.year = year;
        this.seller = seller;
    }
}
