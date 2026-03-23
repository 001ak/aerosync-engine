-- V3__add_airport_code.sql

-- Add airport_code to ride_requests
ALTER TABLE ride_requests ADD COLUMN airport_code VARCHAR(10) NOT NULL DEFAULT 'PNQ';

-- Add airport_code to matches (good for analytics and partitioning later)
ALTER TABLE matches ADD COLUMN airport_code VARCHAR(10) NOT NULL DEFAULT 'PNQ';

-- Create an index because we will be querying by airport + status constantly
CREATE INDEX idx_ride_requests_airport_status ON ride_requests(airport_code, status);