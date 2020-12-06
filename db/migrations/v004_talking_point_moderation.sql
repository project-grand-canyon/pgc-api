ALTER TABLE talking_points ADD COLUMN `review_status` enum('promoted', 'passed', 'waiting_review', 'archived') NOT NULL DEFAULT 'waiting_review';
