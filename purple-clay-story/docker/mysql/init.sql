-- Auto-runs on first MySQL container start
CREATE DATABASE IF NOT EXISTS purpleclay_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE purpleclay_db;

-- Hibernate will create tables via ddl-auto: update
-- This just ensures charset is correct for Tamil/Unicode characters
