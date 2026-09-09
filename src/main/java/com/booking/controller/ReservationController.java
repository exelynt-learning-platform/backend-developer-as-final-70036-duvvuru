package com.booking.controller;

import com.booking.dto.ReservationFilterRequest;
import com.booking.dto.ReservationRequest;
import com.booking.dto.ReservationResponse;
import com.booking.dto.ReservationUpdateRequest;
import com.booking.security.AppUserDetails;
import com.booking.service.ReservationService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * USER gets only their own reservations back, ADMIN gets everyone's.
     * Supports ?status=, ?minPrice=, ?maxPrice= plus the usual page/size/sort params
     * (e.g. sort=price,desc).
     */
    @GetMapping
    public Page<ReservationResponse> list(
            @Valid @ParameterObject ReservationFilterRequest filter,
            @ParameterObject @PageableDefault(sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AppUserDetails me) {
        return reservationService.list(me, filter.status(), filter.minPrice(), filter.maxPrice(), pageable);
    }

    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable long id,
                                       @AuthenticationPrincipal AppUserDetails me) {
        return reservationService.getById(me, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request,
                                      @AuthenticationPrincipal AppUserDetails me) {
        return reservationService.create(me, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ReservationResponse update(@PathVariable long id,
                                      @Valid @RequestBody ReservationUpdateRequest request) {
        return reservationService.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable long id,
                                      @AuthenticationPrincipal AppUserDetails me) {
        return reservationService.cancel(me, id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        reservationService.delete(id);
    }
}
