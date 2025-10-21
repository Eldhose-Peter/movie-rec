-- Connect to the movie_db database
\c movie_db;

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


