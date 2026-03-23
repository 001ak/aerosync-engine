package com.aerosync.repository;

import com.aerosync.domain.entity.Match;
import com.aerosync.domain.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    // SDE2 Corner Case: We need a cron job to auto-expire matches if users don't
    // click 'Accept' within the 1-2 minute window. This query finds them.
    @Query("SELECT m FROM Match m WHERE m.status IN ('PROPOSED', 'ACCEPTED_A', 'ACCEPTED_B') AND m.expiresAt < :now")
    List<Match> findExpiredPendingMatches(@Param("now") Instant now);

    // Checks if a specific ride request is already part of an active match process
    @Query("SELECT COUNT(m) > 0 FROM Match m WHERE (m.requestA.id = :requestId OR m.requestB.id = :requestId) AND m.status IN ('PROPOSED', 'ACCEPTED_A', 'ACCEPTED_B', 'CONFIRMED')")
    boolean isRequestCurrentlyMatched(@Param("requestId") UUID requestId);
}