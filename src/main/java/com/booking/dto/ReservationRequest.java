package com.booking.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationRequest(

        @NotNull(message = "resourceId is required")
        Long resourceId,

        @NotNull(message = "startTime is required")
        @FutureOrPresent(message = "startTime must be now or in the future")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
        LocalDateTime endTime,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "price can have at most 8 digits and 2 decimals")
        BigDecimal price) {
}
