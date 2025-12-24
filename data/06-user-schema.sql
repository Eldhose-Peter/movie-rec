-- Connect to the recommendation_db database
\c user_db;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER SEQUENCE users_id_seq RESTART WITH 140000;


-- Create the ratings table
CREATE TABLE internal_ratings (
    rater_id INT,
    movie_id INT,
    rating NUMERIC(3, 1) NOT NULL,
    time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rater_id, movie_id)
);

ALTER TABLE ratings
ADD CONSTRAINT ratings_rating_check CHECK (rating >= 0 AND rating <= 10); 
