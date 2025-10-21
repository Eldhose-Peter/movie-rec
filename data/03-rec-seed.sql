-- Connect to the recommendation_db database
\c recommendation_db;

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
