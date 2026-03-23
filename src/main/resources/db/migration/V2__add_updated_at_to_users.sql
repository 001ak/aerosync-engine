-- V2__add_updated_at_to_users.sql

-- Add the missing column with a default value so existing rows don't break
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;