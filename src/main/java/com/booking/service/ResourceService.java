package com.booking.service;

import com.booking.dto.ResourceRequest;
import com.booking.dto.ResourceResponse;
import com.booking.exception.ConflictException;
import com.booking.exception.NotFoundException;
import com.booking.model.Resource;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResourceService {

    private final ResourceRepository resources;
    private final ReservationRepository reservations;

    public ResourceService(ResourceRepository resources, ReservationRepository reservations) {
        this.resources = resources;
        this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> list(Pageable pageable) {
        return resources.findAll(pageable).map(ResourceResponse::from);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getById(long id) {
        return ResourceResponse.from(findOrThrow(id));
    }

    public ResourceResponse create(ResourceRequest request) {
        Resource resource = new Resource(request.name(), request.type(), request.description(), request.capacity());
        return ResourceResponse.from(resources.save(resource));
    }

    public ResourceResponse update(long id, ResourceRequest request) {
        Resource resource = findOrThrow(id);
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setDescription(request.description());
        resource.setCapacity(request.capacity());
        return ResourceResponse.from(resource);
    }

    public void delete(long id) {
        Resource resource = findOrThrow(id);
        if (reservations.existsByResourceId(id)) {
            throw new ConflictException("Resource still has reservations, delete or cancel those first");
        }
        resources.delete(resource);
    }

    private Resource findOrThrow(long id) {
        return resources.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
    }
}
