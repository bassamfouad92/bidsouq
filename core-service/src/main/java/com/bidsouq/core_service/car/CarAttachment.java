package com.bidsouq.core_service.car;

import com.bidsouq.core_service.common.enums.AttachmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "car_attachments",
        indexes = {
                @Index(name = "idx_attachment_car", columnList = "car_id")
        }
)
@Setter
@Getter
@NoArgsConstructor
public class CarAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttachmentType type;

    private Integer displayOrder;

    @Column(length = 255)
    private String caption;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    public CarAttachment(String url, AttachmentType type, Integer displayOrder, String caption) {
        this.url = url;
        this.type = type;
        this.displayOrder = displayOrder;
        this.caption = caption;
    }
}
