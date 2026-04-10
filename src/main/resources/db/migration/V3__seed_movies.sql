INSERT INTO movies (title, description, release_year, duration_minutes, country)
VALUES ('The Dark Knight', 'Batman faces the Joker in Gotham City.', 2008, 152, 'USA'),
       ('Inception', 'A skilled thief enters dreams to steal secrets.', 2010, 148, 'USA'),
       ('Interstellar', 'A team travels through a wormhole to save humanity.', 2014, 169, 'USA'),
       ('Parasite', 'A poor family infiltrates the household of a wealthy family.', 2019, 132, 'South Korea'),
       ('Spirited Away', 'A young girl enters a mysterious spirit world.', 2001, 125, 'Japan'),
       ('The Prestige', 'Two rival magicians engage in a dangerous battle of obsession.', 2006, 130, 'USA');

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Action'
WHERE m.title = 'The Dark Knight';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Crime'
WHERE m.title = 'The Dark Knight';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Drama'
WHERE m.title = 'The Dark Knight';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Sci-Fi'
WHERE m.title = 'Inception';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Thriller'
WHERE m.title = 'Inception';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Sci-Fi'
WHERE m.title = 'Interstellar';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Drama'
WHERE m.title = 'Interstellar';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Drama'
WHERE m.title = 'Parasite';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Thriller'
WHERE m.title = 'Parasite';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Animation'
WHERE m.title = 'Spirited Away';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Fantasy'
WHERE m.title = 'Spirited Away';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Drama'
WHERE m.title = 'The Prestige';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Mystery'
WHERE m.title = 'The Prestige';

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Thriller'
WHERE m.title = 'The Prestige';