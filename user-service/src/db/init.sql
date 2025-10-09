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

CREATE TABLE movie_genres (
    movie_id INT,
    genre_id INT,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id),
    FOREIGN KEY (genre_id) REFERENCES genres(id)
);

-- Load data into genres table
COPY genres(id, name)
FROM '/docker-entrypoint-initdb.d/genres.csv'
DELIMITER ','
CSV HEADER;

-- Load data into movies table
COPY movies(id, title, original_title, release_date, overview, popularity, vote_count, vote_average, original_language, backdrop_path, poster_path, adult, video)
FROM '/docker-entrypoint-initdb.d/movies.csv'
DELIMITER ','
CSV HEADER;

-- Load data into movie_genres table
COPY movie_genres(movie_id, genre_id)
FROM '/docker-entrypoint-initdb.d/movies-genres.csv'
DELIMITER ','
CSV HEADER;


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

-- Create a temporary staging table
CREATE TEMP TABLE ratings_staging (
    rater_id INT,
    movie_id INT,
    rating NUMERIC(3, 1),
    time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Copy the data into the staging table
COPY ratings_staging(rater_id, movie_id, rating)
FROM '/docker-entrypoint-initdb.d/ratings.csv'
DELIMITER ','
CSV HEADER;

-- Insert into the main table, resolving conflicts
INSERT INTO ratings (rater_id, movie_id, rating, time)
SELECT DISTINCT ON (rater_id, movie_id) rater_id, movie_id, rating, CURRENT_TIMESTAMP
FROM ratings_staging
ORDER BY rater_id, movie_id, time DESC
ON CONFLICT (rater_id, movie_id)
DO UPDATE SET 
    rating = EXCLUDED.rating,
    time = EXCLUDED.time;
