package com.booking.dto;

import com.booking.model.Resource;

public record ResourceResponse(Long id, String name, String type, String description, Integer capacity) {

    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getDescription(),
                resource.getCapacity());
    }
}
