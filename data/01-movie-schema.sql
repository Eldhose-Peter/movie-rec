-- Connect to the movie_db database
\c movie_db;

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

