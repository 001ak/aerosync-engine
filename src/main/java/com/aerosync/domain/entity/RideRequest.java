package com.aerosync.domain.entity;

import com.aerosync.domain.enums.GenderPreference;
import com.aerosync.domain.enums.RequestType;
import com.aerosync.domain.enums.RideRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.locationtech.jts.geom.Point; // Ensure this is from JTS!

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ride_requests")
@Getter
@Setter
@NoArgsConstructor
public class RideRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // CRITICAL: FetchType.LAZY prevents loading the User every time we query requests
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", length = 20)
    private RequestType requestType;

    @Column(name = "flight_number", length = 20)
    private String flightNumber;

    @Column(name = "flight_date")
    private LocalDate flightDate;

    // The PostGIS spatial column
    @Column(name = "drop_location", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point dropLocation;

    @Column(name = "drop_address_text", nullable = false, columnDefinition = "TEXT")
    private String dropAddressText;

    @Column(name = "handbags_count", nullable = false)
    private int handbagsCount = 0;

    @Column(name = "trolleys_count", nullable = false)
    private int trolleysCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", length = 20)
    private GenderPreference genderPreference;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RideRequestStatus status;

    @Column(name = "airport_code", nullable = false, length = 10)
    private String airportCode;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        RideRequest that = (RideRequest) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}