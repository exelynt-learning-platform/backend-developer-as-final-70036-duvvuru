package com.booking.dto;

import com.booking.model.ReservationStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationUpdateRequest(

        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
        LocalDateTime endTime,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "price can have at most 8 digits and 2 decimals")
        BigDecimal price,

        @NotNull(message = "status is required")
        ReservationStatus status) {
}
