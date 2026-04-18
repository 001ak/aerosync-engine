# AeroSync Engine

> **A highly concurrent, multi-tenant backend engine for airport ride-sharing and dynamic passenger matching.**

## 📖 Project Overview
AeroSync Engine is a B2B2C backend system designed for airport prepaid taxi fleet operators. Unlike standard ride-hailing apps that suffer from booking friction and payment disputes, AeroSync groups landed passengers based on real-time flight data, luggage constraints, and intelligent route deviations, dispatching them as a single optimized ride.

This repository contains the core matching engine, the distributed handshake logic, and the real-time rendezvous architecture.

*(Note: Payment and escrow handling modules are decoupled and out of scope for this core matching service).*

## 🏗️ Technical Architecture & Stack
The system is built to handle the "airport surge" — massive spikes of concurrent users landing from multiple flights simultaneously.

* **Core Framework:** Java 17, Spring Boot 3
* **Databases:** PostgreSQL (Relational Data), PostGIS (Geospatial Extension)
* **In-Memory Store & Caching:** Redis
* **Messaging & Real-Time:** WebSockets, RabbitMQ / Apache Kafka
* **Design Patterns:** Distributed Locking, Event-Driven Architecture, State Machines, Multi-Tenancy

## 🚀 Core Engineering Challenges & Solutions

### 1. The Concurrency Problem: Distributed Handshake
**The Challenge:** When a match is proposed, both users have a 60-second window to "Accept." If Users A, B, and C all accept overlapping matches at the exact same millisecond, the system must prevent race conditions and illegal state transitions.
**The Solution:** * Implemented a concurrent double-opt-in matching engine utilizing **Redis distributed locks**.
* The system atomically serializes multi-user race conditions, ensuring that if A and B lock successfully, C is gracefully returned to the active queue.
* *Result:* Sustains simulated traffic spikes of 500+ simultaneous handshakes with zero state collisions or deadlocks.

### 2. Geospatial Optimization: Dynamic Route Deviation
**The Challenge:** Matching passengers isn't just about exact destinations. The algorithm must calculate if User A's drop-off is on the way to User B's destination, adding less than 10 minutes to the total trip, while factoring in car luggage capacity.
**The Solution:**
* Integrated **PostgreSQL with the PostGIS extension** to push complex geospatial math down to the database layer.
* Utilized **GiST (Generalized Search Tree) spatial indexing** for high-speed polygon routing and intersection queries.
* *Result:* Reduced geospatial match-query latency to `<50ms` across complex constraints.

### 3. Ephemeral Real-Time State: The Rendezvous
**The Challenge:** Once matched, users need to find each other at a crowded airport terminal. They need a live map showing each other's location, but this data is highly ephemeral and shouldn't bloat persistent storage.
**The Solution:**
* Architected a real-time location-sharing system using **WebSockets** backed by **Redis Pub/Sub**.
* Implemented strict TTLs (Time To Live) and automated state teardown. The session self-destructs the moment the users meet or after a 20-minute timeout.
* *Result:* Maintained bi-directional communication with `<100ms` latency without leaking memory or taxing the primary SQL database.

## 🗄️ Core Database Architecture (Multi-Tenant)
The database schema strictly isolates data using a `tenant_id` to support multiple fleet operators securely. State transitions are rigorously managed.

* **`users`**: Manages passenger profiles and verification states.
* **`ride_requests`**: The central pool. Utilizes `drop_location geometry(Point, 4326)` for PostGIS queries.
    * *State Machine:* `SCHEDULED` ➔ `ACTIVE` ➔ `LOCKED_FOR_MATCH` ➔ `MATCHED` | `EXPIRED` | `CANCELLED`
* **`matches`**: Handles the critical 60-second double-opt-in window. A unique partial index prevents duplicate active match proposals.
    * *State Machine:* `PROPOSED` ➔ `ACCEPTED_A`/`ACCEPTED_B` ➔ `CONFIRMED` | `REJECTED` | `EXPIRED`

## 📡 Decoupled Event Triggers
To ensure high availability during traffic surges, the matching engine is decoupled from external volatility (like flight-tracking APIs). **RabbitMQ/Kafka** is utilized to handle asynchronous events such as flight delays, fleet dispatch webhooks, and push notifications, preventing the main application threads from blocking.