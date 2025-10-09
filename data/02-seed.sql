-- Connect to the node-postgres-demo database
\c node-postgres-demo;

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

-- Drop the temporary staging table
DROP TABLE ratings_staging;
