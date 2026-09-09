package com.booking.dto;

import com.booking.model.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record ReservationFilterRequest(
        ReservationStatus status,

        @DecimalMin(value = "0.0", message = "minPrice cannot be negative")
        BigDecimal minPrice,

        @DecimalMin(value = "0.0", message = "maxPrice cannot be negative")
        BigDecimal maxPrice) {
}
