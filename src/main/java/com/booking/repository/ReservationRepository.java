package com.booking.repository;

import com.booking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    boolean existsByResourceId(Long resourceId);

    // two ranges overlap when existing.start < new.end AND existing.end > new.start
    @Query("""
            select count(r) > 0 from Reservation r
            where r.resource.id = :resourceId
              and r.status <> com.booking.model.ReservationStatus.CANCELLED
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    boolean hasOverlap(@Param("resourceId") Long resourceId,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.resource.id = :resourceId
              and r.id <> :reservationId
              and r.status <> com.booking.model.ReservationStatus.CANCELLED
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    boolean hasOverlapExcluding(@Param("resourceId") Long resourceId,
                                @Param("reservationId") Long reservationId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
