-- Connect to the node-postgres-demo database
\c node-postgres-demo;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER SEQUENCE users_id_seq RESTART WITH 140000;

-- Create genres table
CREATE TABLE genres (
    id INT PRIMARY KEY,
    name VARCHAR(100)
);

-- Create the movies table
CREATE TABLE movies (
    id INT PRIMARY KEY,
    title VARCHAR(255),
    original_title VARCHAR(255),
    release_date DATE,
    overview TEXT,
    popularity DECIMAL(10, 3),
    vote_count INT,
    vote_average DECIMAL(4, 2),
    original_language CHAR(2),
    backdrop_path VARCHAR(255),
    poster_path VARCHAR(255),
    adult BOOLEAN,
    video BOOLEAN
);

-- Create the movie_genres junction table
CREATE TABLE movie_genres (
    movie_id INT,
    genre_id INT,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id),
    FOREIGN KEY (genre_id) REFERENCES genres(id)
);


-- Create the ratings table
CREATE TABLE ratings (
    rater_id INT,
    movie_id INT,
    rating NUMERIC(3, 1) NOT NULL,
    time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rater_id, movie_id)
);

ALTER TABLE ratings
ADD CONSTRAINT ratings_rating_check CHECK (rating >= 0 AND rating <= 10); 

-- Create user signature table
CREATE TABLE user_signature (
    rater_id INT PRIMARY KEY,
    signature INT[] NOT NULL
);

-- Create LSH buckets table
CREATE TABLE lsh_bucket (
    bucket_id BIGINT,
    rater_id INT,
    PRIMARY KEY (bucket_id, rater_id)
);

-- Create candidate pairs table
CREATE TABLE similarity_candidate (
    rater_id INT,
    other_rater_id INT,
    PRIMARY KEY (rater_id, other_rater_id)
);
    

-- Create user similarity table
CREATE TABLE user_similarity (
    rater_id INT,
    other_rater_id INT,
    similarity_score FLOAT,
    PRIMARY KEY (rater_id, other_rater_id)
);

-- Create recommendations table
CREATE TABLE user_recommendations (
    user_id INT NOT NULL,
    movie_id INT NOT NULL,
    weighted_sum_total DOUBLE PRECISION DEFAULT 0,
    weight_total DOUBLE PRECISION DEFAULT 0,
    predicted_rating DOUBLE PRECISION GENERATED ALWAYS AS
        (CASE WHEN weight_total > 0 
              THEN weighted_sum_total / weight_total 
              ELSE 0 END) STORED,
    PRIMARY KEY (user_id, movie_id)
);


