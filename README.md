# Resource Booking API

REST API for booking shared resources (meeting rooms, vehicles, equipment) with JWT-based
auth and two roles:

- **USER** can browse resources and create/cancel/view **their own** reservations.
- **ADMIN** manages resources (full CRUD) and can see/update/cancel/delete **all** reservations.

Built with Java 17, Spring Boot 3.3, Spring Security (stateless JWT), Spring Data JPA/Hibernate
and MySQL. Swagger UI included. Tests run on in-memory H2, so `mvn test` needs nothing but Maven.

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for MySQL) — or any local MySQL 8 instance

## Setup

```bash
# 1. start MySQL (creates db "bookingdb", user/pass booking/booking)
docker compose up -d

# 2. run the app
mvn spring-boot:run
```

The app starts on http://localhost:8080 and seeds demo data on first run
(only when the DB is empty, so restarts don't duplicate anything).

Swagger UI: http://localhost:8080/swagger-ui.html —
log in via `POST /auth/login`, copy `accessToken`, click **Authorize**, paste it as `Bearer <token>`.

### Tests

```bash
mvn test
```

## Configuration

Everything has a working default for local dev; override via environment variables:

| Variable            | Default                                                        | Purpose                          |
|---------------------|----------------------------------------------------------------|----------------------------------|
| `DB_URL`            | `jdbc:mysql://localhost:3306/bookingdb?createDatabaseIfNotExist=true` | JDBC URL                   |
| `DB_USER`           | `booking`                                                      | DB username                      |
| `DB_PASS`           | `booking`                                                      | DB password                      |
| `JWT_SECRET`        | dev-only default                                               | HMAC secret, must be ≥ 32 chars  |
| `JWT_EXPIRATION_MS` | `86400000` (24h)                                               | token lifetime                   |

Prefer PostgreSQL? Swap the MySQL driver for `org.postgresql:postgresql` in `pom.xml` and set
`DB_URL=jdbc:postgresql://localhost:5432/bookingdb` — no code changes needed.

## Seed users

| Email               | Password   | Role  |
|---------------------|------------|-------|
| `admin@booking.com` | `admin123` | ADMIN |
| `user@booking.com`  | `user123`  | USER  |
| `priya@booking.com` | `user123`  | USER  |

A few resources and reservations are seeded too (PENDING/CONFIRMED/CANCELLED, various prices)
so filtering and pagination can be tried immediately.

## API overview

| Method   | Path                          | Access        | Description                                   |
|----------|-------------------------------|---------------|-----------------------------------------------|
| `POST`   | `/auth/login`                 | public        | log in, returns JWT                           |
| `GET`    | `/api/resources`              | USER + ADMIN  | list resources (paginated)                    |
| `GET`    | `/api/resources/{id}`         | USER + ADMIN  | resource details                              |
| `POST`   | `/api/resources`              | ADMIN         | create resource                               |
| `PUT`    | `/api/resources/{id}`         | ADMIN         | update resource                               |
| `DELETE` | `/api/resources/{id}`         | ADMIN         | delete resource (409 if it has reservations)  |
| `GET`    | `/api/reservations`           | USER + ADMIN  | list reservations — USER sees only their own  |
| `GET`    | `/api/reservations/{id}`      | owner / ADMIN | single reservation                            |
| `POST`   | `/api/reservations`           | USER + ADMIN  | create reservation (status starts PENDING)    |
| `PUT`    | `/api/reservations/{id}`      | ADMIN         | update times/price/status                     |
| `PATCH`  | `/api/reservations/{id}/cancel` | owner / ADMIN | cancel a reservation                        |
| `DELETE` | `/api/reservations/{id}`      | ADMIN         | delete a reservation                          |

`GET /api/reservations` query params:

- `status` — `PENDING` | `CONFIRMED` | `CANCELLED`
- `minPrice`, `maxPrice` — decimal range filter on price
- `page` (0-based), `size` — pagination
- `sort` — e.g. `sort=price,desc`, repeatable (`&sort=startTime,asc`)

### Quick try (curl)

```bash
# log in
TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@booking.com","password":"user123"}' | jq -r .accessToken)

# browse resources
curl -s localhost:8080/api/resources -H "Authorization: Bearer $TOKEN"

# book Meeting Room B for tomorrow 10:00-12:00
curl -s -X POST localhost:8080/api/reservations \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"resourceId":2,"startTime":"2026-09-01T10:00:00","endTime":"2026-09-01T12:00:00","price":300.00}'

# filter + sort + paginate your reservations
curl -s "localhost:8080/api/reservations?status=PENDING&minPrice=100&maxPrice=1000&page=0&size=5&sort=price,desc" \
  -H "Authorization: Bearer $TOKEN"
```

## Design notes / decisions

- **Identity comes from the JWT, always.** The create-reservation body has no `userId` field on
  purpose; the owner is taken from the authenticated principal (`AppUserDetails`).
- **403 vs 404 for other people's data:** a USER asking for someone else's reservation gets
  `404 Not Found`, so the API doesn't even confirm the reservation exists. RBAC violations
  (e.g. USER trying to create a resource) get a proper `403`.
- **Double bookings** are rejected with `409 Conflict` — overlapping time ranges on the same
  resource can't be reserved (CANCELLED reservations don't block).
- **Price** is stored as `DECIMAL(10,2)` and validated (> 0, max 2 decimals). It's taken from the
  request per the assignment spec; in production I'd compute it server-side from a per-resource rate.
- **`ddl-auto: update`** keeps setup zero-effort for the evaluator; a real project would use
  Flyway/Liquibase migrations instead.
- Passwords are BCrypt-hashed. Login returns the same 401 message for unknown email and wrong
  password so you can't enumerate registered emails.
- Stateless sessions (`SessionCreationPolicy.STATELESS`), CSRF disabled — there's no cookie-based
  session to protect, all auth is via the `Authorization: Bearer` header.

## Project structure

```
src/main/java/com/booking
├── config            # security filter chain, OpenAPI, data seeder
├── controller        # REST endpoints (thin, delegate to services)
├── dto               # request/response records, error payload
├── exception         # NotFound/Conflict + @RestControllerAdvice
├── model             # JPA entities + enums (Role, ReservationStatus)
├── repository        # Spring Data repos + JPA specification for filters
├── security          # JWT service/filter, UserDetails, 401/403 handlers
└── service           # business logic, ownership checks, transactions
```

Error responses are consistent JSON:

```json
{
  "timestamp": "2026-08-31T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/reservations",
  "fieldErrors": { "price": "price must be greater than zero" }
}
```
