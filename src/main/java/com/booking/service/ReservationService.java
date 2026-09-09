package com.booking.service;

import com.booking.dto.ReservationRequest;
import com.booking.dto.ReservationResponse;
import com.booking.dto.ReservationUpdateRequest;
import com.booking.exception.ConflictException;
import com.booking.exception.NotFoundException;
import com.booking.model.Reservation;
import com.booking.model.ReservationStatus;
import com.booking.model.Resource;
import com.booking.model.Role;
import com.booking.model.User;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ReservationSpecifications;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import com.booking.security.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservations;
    private final ResourceRepository resources;
    private final UserRepository users;

    public ReservationService(ReservationRepository reservations, ResourceRepository resources,
                              UserRepository users) {
        this.reservations = reservations;
        this.resources = resources;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> list(AppUserDetails caller, ReservationStatus status,
                                          BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        // admins can see everything, everyone else is limited to their own bookings
        Long ownerId = caller.getRole() == Role.ADMIN ? null : caller.getId();

        return reservations
                .findAll(ReservationSpecifications.withFilters(ownerId, status, minPrice, maxPrice), pageable)
                .map(ReservationResponse::from);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(AppUserDetails caller, long id) {
        return ReservationResponse.from(findOwnedOrThrow(caller, id));
    }

    public ReservationResponse create(AppUserDetails caller, ReservationRequest request) {
        requireValidRange(request.startTime(), request.endTime());

        Resource resource = resources.findById(request.resourceId())
                .orElseThrow(() -> new NotFoundException("Resource not found"));

        if (reservations.hasOverlap(resource.getId(), request.startTime(), request.endTime())) {
            throw new ConflictException("That resource is already booked for the requested time range");
        }

        // the booking always belongs to the caller from the token, never to a userId from the request body
        User owner = users.getReferenceById(caller.getId());
        Reservation saved = reservations.save(
                new Reservation(resource, owner, request.startTime(), request.endTime(), request.price()));

        return ReservationResponse.from(saved);
    }

    public ReservationResponse update(long id, ReservationUpdateRequest request) {
        requireValidRange(request.startTime(), request.endTime());

        Reservation reservation = reservations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        if (reservations.hasOverlapExcluding(reservation.getResource().getId(), id,
                request.startTime(), request.endTime())) {
            throw new ConflictException("That resource is already booked for the requested time range");
        }

        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPrice(request.price());
        reservation.setStatus(request.status());
        return ReservationResponse.from(reservation);
    }

    public ReservationResponse cancel(AppUserDetails caller, long id) {
        Reservation reservation = findOwnedOrThrow(caller, id);
        if (reservation.getStatus() != ReservationStatus.CANCELLED) {
            reservation.setStatus(ReservationStatus.CANCELLED);
        }
        return ReservationResponse.from(reservation);
    }

    public void delete(long id) {
        if (!reservations.existsById(id)) {
            throw new NotFoundException("Reservation not found");
        }
        reservations.deleteById(id);
    }

    private Reservation findOwnedOrThrow(AppUserDetails caller, long id) {
        Reservation reservation = reservations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        // 404 instead of 403 on purpose: don't even reveal whether someone else's reservation exists
        if (caller.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(caller.getId())) {
            throw new NotFoundException("Reservation not found");
        }
        return reservation;
    }

    private static void requireValidRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }
}
