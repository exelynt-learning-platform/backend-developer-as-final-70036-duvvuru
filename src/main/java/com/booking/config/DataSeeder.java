package com.booking.config;

import com.booking.model.Reservation;
import com.booking.model.ReservationStatus;
import com.booking.model.Resource;
import com.booking.model.Role;
import com.booking.model.User;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Seeds demo data on first start (empty DB only, so restarts don't duplicate anything).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final ResourceRepository resources;
    private final ReservationRepository reservations;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository users, ResourceRepository resources,
                      ReservationRepository reservations, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.resources = resources;
        this.reservations = reservations;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        User admin = users.save(new User("admin@booking.com", passwordEncoder.encode("admin123"), "Site Admin", Role.ADMIN));
        User arjun = users.save(new User("user@booking.com", passwordEncoder.encode("user123"), "Arjun Mehta", Role.USER));
        User priya = users.save(new User("priya@booking.com", passwordEncoder.encode("user123"), "Priya Sharma", Role.USER));

        Resource roomA = resources.save(new Resource("Meeting Room A", "ROOM", "8-seater on the first floor, has a TV", 8));
        Resource roomB = resources.save(new Resource("Meeting Room B", "ROOM", "Small 4-seater near reception", 4));
        Resource innova = resources.save(new Resource("Toyota Innova", "VEHICLE", "Company car, KA-01-AB-1234", 6));
        Resource projector = resources.save(new Resource("Epson Projector", "EQUIPMENT", "Full HD, comes with an HDMI cable", null));

        // Create sample reservations with start times offset by days from now (daysFromNow parameter in at() method)
        seedReservation(roomA, arjun, at(1, 10), at(1, 12), "499.00", ReservationStatus.PENDING);
        seedReservation(innova, arjun, at(2, 9), at(2, 17), "1200.50", ReservationStatus.CONFIRMED);
        seedReservation(projector, arjun, at(3, 14), at(3, 16), "250.00", ReservationStatus.CANCELLED);
        seedReservation(roomB, priya, at(1, 14), at(1, 15), "800.00", ReservationStatus.PENDING);

        log.info("Seeded demo data. Log in with admin@booking.com / admin123 (ADMIN) "
                + "or user@booking.com / user123, priya@booking.com / user123 (USER)");
    }

    private void seedReservation(Resource resource, User user,
                                 LocalDateTime start, LocalDateTime end,
                                 String price, ReservationStatus status) {
        Reservation r = new Reservation(resource, user, start, end, new BigDecimal(price));
        r.setStatus(status);
        reservations.save(r);
    }

    private static LocalDateTime at(int daysFromNow, int hour) {
        return LocalDateTime.now().plusDays(daysFromNow).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }
}
