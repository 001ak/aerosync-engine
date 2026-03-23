package com.aerosync.repository;

import com.aerosync.domain.entity.RideRequest;
import com.aerosync.domain.enums.RideRequestStatus;
import jakarta.persistence.LockModeType;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, UUID> {

    // ========================================================================
    // 1. CONCURRENCY CONTROL (Pessimistic Locking)
    // ========================================================================
    // SDE2 Concept: When our matching engine selects two requests to match,
    // we call this method. It executes `SELECT ... FOR UPDATE` in PostgreSQL.
    // This locks the rows. If another thread tries to match these users,
    // it will be forced to wait until our transaction commits or rolls back.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RideRequest r WHERE r.id = :id")
    Optional<RideRequest> findByIdForUpdate(@Param("id") UUID id);


    // ========================================================================
    // 2. GEOSPATIAL MATCHING ENGINE (Native PostGIS Query)
    // ========================================================================
    // Why Native? Casting to `::geography` is essential to calculate distance
    // in METERS instead of DEGREES (which happens if we stick to `geometry`).
    // It also perfectly utilizes the GIST index we created in Flyway.
    @Query(value = """
        SELECT * FROM ride_requests r
        WHERE r.tenant_id = :tenantId
          AND r.airport_code = :airportCode      -- <--- NEW HARD FILTER
          AND r.status = 'ACTIVE'
          AND r.id != :requestId
          -- Spatial check: Is User B's drop off within X meters of User A's drop off?
          AND ST_DWithin(r.drop_location::geography, :searchPoint::geography, :radiusInMeters)
          -- Capacity checks: Combined luggage must fit in one standard cab
          AND (r.handbags_count + :userHandbags) <= :maxVehicleHandbags
          AND (r.trolleys_count + :userTrolleys) <= :maxVehicleTrolleys
          -- Gender preference logic (Basic implementation for MVP)
          AND (r.gender_preference = 'ANY' OR r.gender_preference = :userGenderPreference)
        ORDER BY ST_Distance(r.drop_location::geography, :searchPoint::geography) ASC
        LIMIT 10
        """, nativeQuery = true)
    List<RideRequest> findNearbyPotentialMatches(
            @Param("tenantId") String tenantId,
            @Param("airportCode") String airportCode,
            @Param("requestId") UUID requestId,
            @Param("searchPoint") Point searchPoint,
            @Param("radiusInMeters") double radiusInMeters,
            @Param("userHandbags") int userHandbags,
            @Param("maxVehicleHandbags") int maxVehicleHandbags,
            @Param("userTrolleys") int userTrolleys,
            @Param("maxVehicleTrolleys") int maxVehicleTrolleys,
            @Param("userGenderPreference") String userGenderPreference
    );
}