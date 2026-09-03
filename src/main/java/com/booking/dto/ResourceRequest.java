package com.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResourceRequest(

        @NotBlank @Size(max = 120) String name,

        @NotBlank @Size(max = 60) String type,

        @Size(max = 500) String description,

        @Positive Integer capacity) {
}
