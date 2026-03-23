# Project Context: AeroSync Engine (Airport Ride-Sharing SaaS)

## 1. Project Overview
**Goal:** Build a complex, highly concurrent backend system for a B2B2C airport ride-sharing platform. This project serves a dual purpose: it is designed to be a standout portfolio piece for SDE2 (Software Development Engineer 2) backend interviews, and it is structured as a viable Multi-Tenant SaaS that can be pitched to airport prepaid taxi fleet operators.
**Core Concept:** A platform that groups landed airport passengers based on real-time flight data, luggage constraints, and route deviations. It securely holds funds in escrow, calculates dynamic fare splits, and dispatches the grouped passengers to partner fleet operators (bypassing the friction of users booking their own Uber/Ola).

## 2. Business Model & The "Pivot"
Standard "share an Uber" apps fail due to booking friction and payment disputes caused by city traffic.
* **The B2B Pivot:** We partner directly with airport Prepaid Taxi unions or fleet operators. We provide the "Shared Fleet Tech" matching engine.
* **The Payment Fix (Escrow):** Users don't negotiate splits or pay each other. The app calculates the route, adds a 20% traffic buffer, and places a pre-authorization hold on both users' accounts upfront. Once the ride finishes, the final exact amount is settled automatically, and the remainder is refunded.

## 3. Core Product Features (User Journey)
1. **Frictionless Entry (PWA):** The MVP is a Progressive Web App accessed via QR codes at the airport baggage claim (no forced app store downloads).
2. **Intent & Verification:** Users input their destination zone, luggage count (vital for car capacity), and verify via Corporate Email/LinkedIn for trust.
3. **Smart Matching (Route Deviation):** Matches aren't just exact destinations. The algorithm matches users if Point P (User A) is on the way to Destination B (User B), provided the deviation adds less than 10 minutes to the total trip.
4. **The Handshake (Double Opt-In):** The system proposes a match. Both users have 60 seconds to click "Accept."
5. **Ephemeral Live Location:** Once accepted, a temporary WebSocket connection opens showing live moving dots of both users on an airport map. This self-destructs the moment they meet or after 20 minutes.
6. **The "FOMO" Engine:** Unmatched users are shown stats like *"14 other people matched from your flight,"* keeping retention high.

## 4. Technical Architecture & Tech Stack
* **Language/Framework:** Java with Spring Boot.
* **Database:** PostgreSQL with **PostGIS** extension (crucial for geospatial/polygon routing queries).
* **Caching & Real-Time:** Redis (for ephemeral live locations, active sessions, and distributed locking).
* **Message Broker:** RabbitMQ or Kafka (for async events like flight delays, push notifications, and webhooks).
* **Client:** React (Web/PWA) for MVP, eventually wrapped in React Native.

## 5. SDE2 Engineering Challenges (The Interview Highlights)
The backend must handle high-concurrency edge cases:
* **Race Conditions & Distributed Locking:** Using Redis to ensure that if Users A, B, and C all accept a match at the same millisecond, the system safely locks A and B, and cleanly returns C to the queue without throwing errors.
* **Idempotency in Payments:** The Escrow State Machine (`AWAITING_FUNDS` ➔ `HELD` ➔ `DISPERSED`) must guarantee that even if network drops cause 5 duplicate requests, a user is only charged or refunded exactly once.
* **Event-Driven Resilience:** Using RabbitMQ to decouple the matching engine from external APIs (like flight tracking) so 500 people landing simultaneously don't crash the main threads.
* **Multi-Tenancy:** Data architecture must use `tenant_id` to isolate operations so the software can be sold to multiple different fleet operators securely.

## 6. Implementation Roadmap
* **Phase 1: Geospatial Engine:** Spring Boot + PostgreSQL/PostGIS setup. Logic for 10-minute route deviation matching and luggage validation.
* **Phase 2: Concurrency & Handshake:** Redis distributed locks for the 60-second acceptance window. WebSockets for temporary location sharing.
* **Phase 3: Escrow State Machine:** Building the idempotent payment logic (Hold -> Calculate final fare -> Refund buffer).
* **Phase 4: Event-Driven Triggers:** Implement RabbitMQ for async flight delay handling and fleet dispatch webhooks.
* **Phase 5: Client App:** Build the React PWA to consume the APIs.

## 7. Database Architecture (Implemented)
The database is PostgreSQL with the PostGIS extension. Schema migrations are handled via Flyway. The architecture supports multi-tenancy (`tenant_id`) and relies heavily on state machines to manage concurrent matching.

### Core Tables:
* **`users`**: Stores passenger details.
    * *Key Columns:* `id` (UUID), `tenant_id`, `gender`, `is_verified`.
    * *Audit Columns:* `created_at`, `updated_at` (Managed automatically via JPA `@EntityListeners`).
* **`ride_requests`**: The central pool for both `ON_DEMAND` (at the airport) and `PRE_BOOKED` (scheduled) requests.
    * *Spatial Data:* Uses `drop_location geometry(Point, 4326)` to store exact coordinates. Indexed using a `GIST` spatial index for high-speed routing queries.
    * *Matching Constraints:* `handbags_count`, `trolleys_count`, `gender_preference`, `airportCode`.
    * *State Machine (`status`):* Transitions through `SCHEDULED` ➔ `ACTIVE` ➔ `LOCKED_FOR_MATCH` ➔ `MATCHED` | `EXPIRED` | `CANCELLED`.
    * *Audit Columns:* `created_at`, `updated_at`.
* **`matches`**: Handles the 1-2 minute double-opt-in handshake.
    * *Key Columns:* `request_a_id`, `request_b_id`, `expires_at`, `chat_room_id`, `airportCode`.
    * *State Machine (`status`):* Transitions through `PROPOSED` ➔ `ACCEPTED_A`/`ACCEPTED_B` ➔ `CONFIRMED` | `REJECTED` | `EXPIRED`.
    * *Concurrency Control:* A unique partial index prevents multiple active match proposals for the same users simultaneously.
    * *Audit Columns:* `created_at`, `updated_at`.