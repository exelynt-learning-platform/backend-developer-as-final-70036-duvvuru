package com.booking.dto;

import com.booking.model.Reservation;
import com.booking.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long resourceId,
        String resourceName,
        Long userId,
        String userEmail,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal price,
        ReservationStatus status,
        LocalDateTime createdAt) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getResource().getId(),
                r.getResource().getName(),
                r.getUser().getId(),
                r.getUser().getEmail(),
                r.getStartTime(),
                r.getEndTime(),
                r.getPrice(),
                r.getStatus(),
                r.getCreatedAt());
    }
}
