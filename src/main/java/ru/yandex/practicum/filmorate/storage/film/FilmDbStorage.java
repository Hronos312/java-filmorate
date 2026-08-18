package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Repository("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT f.film_id,
                   f.name,
                   f.description,
                   f.release_date,
                   f.duration,
                   m.mpa_id,
                   m.name AS mpa_name
            FROM films AS f
            JOIN mpa AS m ON m.mpa_id = f.mpa_id
            ORDER BY f.film_id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT f.film_id,
                   f.name,
                   f.description,
                   f.release_date,
                   f.duration,
                   m.mpa_id,
                   m.name AS mpa_name
            FROM films AS f
            JOIN mpa AS m ON m.mpa_id = f.mpa_id
            WHERE f.film_id = ?
            """;

    private static final String INSERT_QUERY = """
            INSERT INTO films (
                name,
                description,
                release_date,
                duration,
                mpa_id
            )
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_QUERY = """
            UPDATE films
            SET name = ?,
                description = ?,
                release_date = ?,
                duration = ?,
                mpa_id = ?
            WHERE film_id = ?
            """;

    private static final String DELETE_QUERY = """
            DELETE FROM films
            WHERE film_id = ?
            """;

    private static final String INSERT_GENRE_QUERY = """
            INSERT INTO film_genres (film_id, genre_id)
            VALUES (?, ?)
            """;

    private static final String DELETE_GENRES_QUERY = """
            DELETE FROM film_genres
            WHERE film_id = ?
            """;

    private static final String MPA_EXISTS_QUERY = """
            SELECT COUNT(*)
            FROM mpa
            WHERE mpa_id = ?
            """;

    private static final String GENRE_EXISTS_QUERY = """
            SELECT COUNT(*)
            FROM genres
            WHERE genre_id = ?
            """;

    private static final String ADD_LIKE_QUERY = """
            MERGE INTO film_likes (film_id, user_id)
            KEY (film_id, user_id)
            VALUES (?, ?)
            """;

    private static final String REMOVE_LIKE_QUERY = """
            DELETE FROM film_likes
            WHERE film_id = ?
              AND user_id = ?
            """;

    private static final String FIND_POPULAR_QUERY = """
            SELECT f.film_id,
                   f.name,
                   f.description,
                   f.release_date,
                   f.duration,
                   m.mpa_id,
                   m.name AS mpa_name,
                   COUNT(fl.user_id) AS likes_count
            FROM films AS f
            JOIN mpa AS m ON m.mpa_id = f.mpa_id
            LEFT JOIN film_likes AS fl ON fl.film_id = f.film_id
            GROUP BY f.film_id,
                     f.name,
                     f.description,
                     f.release_date,
                     f.duration,
                     m.mpa_id,
                     m.name
            ORDER BY likes_count DESC,
                     f.film_id
            LIMIT ?
            """;

    private static final String FIND_GENRES_FOR_FILMS_QUERY = """
            SELECT fg.film_id,
                   g.genre_id,
                   g.name
            FROM film_genres AS fg
            JOIN genres AS g
              ON g.genre_id = fg.genre_id
            WHERE fg.film_id IN (%s)
            ORDER BY fg.film_id, g.genre_id
            """;

    private static final String FIND_LIKES_FOR_FILMS_QUERY = """
            SELECT film_id,
                   user_id
            FROM film_likes
            WHERE film_id IN (%s)
            ORDER BY film_id, user_id
            """;

    private static final String FIND_RECOMMENDATION_FILMS_BY_USER = """
            SELECT f.*, m.name AS mpa_name
            FROM films f
            JOIN mpa AS m ON f.mpa_id = m.mpa_id
            WHERE f.film_id IN (
                SELECT fl_similar.film_id
                FROM film_likes AS fl_similar
                WHERE fl_similar.user_id = (
                    SELECT fl2.user_id
                    FROM film_likes AS fl1
                    JOIN film_likes AS fl2 ON fl1.film_id = fl2.film_id AND fl1.user_id <> fl2.user_id
                    WHERE fl1.user_id = ?
                    GROUP BY fl2.user_id
                    ORDER BY COUNT(fl2.film_id) DESC
                    LIMIT 1
                )
                AND fl_similar.film_id NOT IN (
                    SELECT fl_target.film_id
                    FROM film_likes AS fl_target
                    WHERE fl_target.user_id = ?
                )
            );
        """;

    private final JdbcTemplate jdbc;
    private final GenreStorage genreStorage;

    public FilmDbStorage(JdbcTemplate jdbc,
                         GenreStorage genreStorage) {
        this.jdbc = jdbc;
        this.genreStorage = genreStorage;
    }

    @Override
    public Collection<Film> findAll() {
        List<Film> films = jdbc.query(FIND_ALL_QUERY, this::mapRow);
        loadRelations(films);

        return films;
    }

    @Override
    public Film findById(Long id) {
        List<Film> films = jdbc.query(FIND_BY_ID_QUERY, this::mapRow, id);

        Film film = films.stream()
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException("Фильм с id " + id + " не найден"));

        loadRelations(List.of(film));

        return film;
    }

    @Override
    @Transactional
    public Film create(Film film) {
        validateMpa(film);
        validateGenres(film.getGenres());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);

            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());
            statement.setLong(5, film.getMpa().getId());

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Не удалось получить id созданного фильма");
        }

        film.setId(generatedId.longValue());

        saveGenres(film);

        return findById(film.getId());
    }

    @Override
    @Transactional
    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("id должен быть указан");
        }

        findById(film.getId());

        validateMpa(film);
        validateGenres(film.getGenres());

        int updatedRows = jdbc.update(
                UPDATE_QUERY,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }

        jdbc.update(DELETE_GENRES_QUERY, film.getId());
        saveGenres(film);

        return findById(film.getId());
    }

    @Override
    public void delete(Long id) {
        int deletedRows = jdbc.update(DELETE_QUERY, id);

        if (deletedRows == 0) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
    }

    private Film mapRow(java.sql.ResultSet resultSet, int rowNum)
            throws java.sql.SQLException {

        Film film = new Film();

        film.setId(resultSet.getLong("film_id"));
        film.setName(resultSet.getString("name"));
        film.setDescription(resultSet.getString("description"));
        film.setReleaseDate(
                resultSet.getDate("release_date").toLocalDate()
        );
        film.setDuration(resultSet.getInt("duration"));

        Mpa mpa = new Mpa();
        mpa.setId(resultSet.getLong("mpa_id"));
        mpa.setName(resultSet.getString("mpa_name"));

        film.setMpa(mpa);

        return film;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbc.update(ADD_LIKE_QUERY, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        jdbc.update(REMOVE_LIKE_QUERY, filmId, userId);
    }

    @Override
    public Collection<Film> findPopular(Integer count) {
        List<Film> films = jdbc.query(
                FIND_POPULAR_QUERY,
                this::mapRow,
                count
        );

        loadRelations(films);

        return films;
    }

    @Override
    public Collection<Film> getRecommendations(Long userId) {
        List<Film> recommendations = jdbc.query(FIND_RECOMMENDATION_FILMS_BY_USER, this::mapRow, userId, userId);

        genreStorage.loadGenresForFilms(recommendations);

        return recommendations;
    }

    private void loadRelations(Collection<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmsById = new HashMap<>();

        for (Film film : films) {
            filmsById.put(film.getId(), film);

            film.getGenres().clear();
            film.getLikes().clear();
        }

        String placeholders = String.join(", ", Collections.nCopies(filmsById.size(), "?"));

        Object[] filmIds = filmsById.keySet().toArray();

        String genresQuery = FIND_GENRES_FOR_FILMS_QUERY.formatted(placeholders);

        List<Map.Entry<Long, Genre>> genreRows = jdbc.query(
                genresQuery,
                (resultSet, rowNum) -> {
                    Genre genre = new Genre();
                    genre.setId(resultSet.getLong("genre_id"));
                    genre.setName(resultSet.getString("name"));

                    Long filmId = resultSet.getLong("film_id");

                    return Map.entry(filmId, genre);
                },
                filmIds
        );

        for (Map.Entry<Long, Genre> row : genreRows) {
            Film film = filmsById.get(row.getKey());

            if (film != null) {
                film.getGenres().add(row.getValue());
            }
        }

        String likesQuery = FIND_LIKES_FOR_FILMS_QUERY.formatted(placeholders);

        List<Map.Entry<Long, Long>> likeRows = jdbc.query(
                likesQuery,
                (resultSet, rowNum) -> Map.entry(
                        resultSet.getLong("film_id"),
                        resultSet.getLong("user_id")
                ),
                filmIds
        );

        for (Map.Entry<Long, Long> row : likeRows) {
            Film film = filmsById.get(row.getKey());

            if (film != null) {
                film.getLikes().add(row.getValue());
            }
        }
    }

    private void saveGenres(Film film) {
        Set<Genre> genres = film.getGenres();

        if (genres == null || genres.isEmpty()) {
            return;
        }

        for (Genre genre : genres) {
            jdbc.update(
                    INSERT_GENRE_QUERY,
                    film.getId(),
                    genre.getId()
            );
        }
    }

    private void validateMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException(
                    "Рейтинг MPA должен быть указан"
            );
        }

        Integer count = jdbc.queryForObject(
                MPA_EXISTS_QUERY,
                Integer.class,
                film.getMpa().getId()
        );

        if (count == null || count == 0) {
            throw new NotFoundException("Рейтинг MPA с id " + film.getMpa().getId() + " не найден");
        }
    }

    private void validateGenres(Set<Genre> genres) {
        if (genres == null) {
            return;
        }

        for (Genre genre : genres) {
            if (genre.getId() == null) {
                throw new ValidationException("Id жанра должен быть указан");
            }

            Integer count = jdbc.queryForObject(
                    GENRE_EXISTS_QUERY,
                    Integer.class,
                    genre.getId()
            );

            if (count == null || count == 0) {
                throw new NotFoundException("Жанр с id " + genre.getId() + " не найден");
            }
        }
    }
}