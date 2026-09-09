package com.booking.repository;

import com.booking.model.Reservation;
import com.booking.model.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    /**
     * Builds the WHERE clause for the reservation list endpoint.
     * ownerId is null when an admin asks (they see everything), otherwise it's the caller's id.
     */
    public static Specification<Reservation> withFilters(Long ownerId, ReservationStatus status,
                                                         BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(4);

            if (ownerId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), ownerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
