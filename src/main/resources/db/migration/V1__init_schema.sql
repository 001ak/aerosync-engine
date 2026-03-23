-- V1__init_schema.sql

-- Enable PostGIS extension (Crucial for geospatial queries)
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL, -- e.g., 'PNQ_PREPAID_TAXI', 'DEL_FLEET_A'
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Ride Requests Table (Handles both Pre-booked and On-Demand)
CREATE TABLE ride_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    tenant_id VARCHAR(50) NOT NULL,

    -- Request Type & Timing
    request_type VARCHAR(20) CHECK (request_type IN ('PRE_BOOKED', 'ON_DEMAND')),
    flight_number VARCHAR(20), -- Nullable for on-demand
    flight_date DATE,          -- Nullable for on-demand

    -- Geospatial Drop Location
    drop_location geometry(Point, 4326) NOT NULL, -- Longitude/Latitude
    drop_address_text TEXT NOT NULL,

    -- Constraints & Preferences
    handbags_count INT DEFAULT 0,
    trolleys_count INT DEFAULT 0,
    gender_preference VARCHAR(20) CHECK (gender_preference IN ('ANY', 'SAME_GENDER')),

    -- State Machine
    status VARCHAR(20) CHECK (status IN ('SCHEDULED', 'ACTIVE', 'LOCKED_FOR_MATCH', 'MATCHED', 'CANCELLED', 'EXPIRED')),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create a spatial index for lightning-fast radius/route queries
CREATE INDEX idx_ride_requests_drop_location ON ride_requests USING GIST (drop_location);
CREATE INDEX idx_ride_requests_status ON ride_requests(status);

-- 3. Matches Table (The Handshake)
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    request_a_id UUID NOT NULL REFERENCES ride_requests(id),
    request_b_id UUID NOT NULL REFERENCES ride_requests(id),

    -- State Machine for the Handshake
    status VARCHAR(20) CHECK (status IN ('PROPOSED', 'ACCEPTED_A', 'ACCEPTED_B', 'CONFIRMED', 'REJECTED', 'EXPIRED')),

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, -- The 1-2 minute timer
    chat_room_id UUID UNIQUE, -- Generated only if status becomes CONFIRMED

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Prevent A and B from being matched multiple times simultaneously
CREATE UNIQUE INDEX idx_unique_active_match ON matches (request_a_id, request_b_id) WHERE status IN ('PROPOSED', 'ACCEPTED_A', 'ACCEPTED_B');