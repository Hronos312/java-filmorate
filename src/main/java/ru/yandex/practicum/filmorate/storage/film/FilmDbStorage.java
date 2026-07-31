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

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    private static final String FIND_GENRES_QUERY = """
            SELECT g.genre_id,
                   g.name
            FROM film_genres AS fg
            JOIN genres AS g ON g.genre_id = fg.genre_id
            WHERE fg.film_id = ?
            ORDER BY g.genre_id
            """;

    private static final String INSERT_GENRE_QUERY = """
            INSERT INTO film_genres (film_id, genre_id)
            VALUES (?, ?)
            """;

    private static final String DELETE_GENRES_QUERY = """
            DELETE FROM film_genres
            WHERE film_id = ?
            """;

    private static final String FIND_LIKES_QUERY = """
            SELECT user_id
            FROM film_likes
            WHERE film_id = ?
            ORDER BY user_id
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

    private final JdbcTemplate jdbc;

    public FilmDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Collection<Film> findAll() {
        List<Film> films = jdbc.query(FIND_ALL_QUERY, this::mapRow);
        films.forEach(this::loadRelations);

        return films;
    }

    @Override
    public Film findById(Long id) {
        List<Film> films = jdbc.query(FIND_BY_ID_QUERY, this::mapRow, id);

        Film film = films.stream()
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException("Фильм с id " + id + " не найден"));

        loadRelations(film);

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

        films.forEach(this::loadRelations);

        return films;
    }

    private Genre mapGenre(java.sql.ResultSet resultSet, int rowNum)
            throws java.sql.SQLException {

        Genre genre = new Genre();

        genre.setId(resultSet.getLong("genre_id"));
        genre.setName(resultSet.getString("name"));

        return genre;
    }

    private void loadRelations(Film film) {
        loadGenres(film);
        loadLikes(film);
    }

    private void loadGenres(Film film) {
        List<Genre> genres = jdbc.query(
                FIND_GENRES_QUERY,
                this::mapGenre,
                film.getId()
        );

        film.setGenres(new LinkedHashSet<>(genres));
    }

    private void loadLikes(Film film) {
        List<Long> likes = jdbc.query(
                FIND_LIKES_QUERY,
                (resultSet, rowNum) -> resultSet.getLong("user_id"),
                film.getId()
        );

        film.getLikes().clear();
        film.getLikes().addAll(likes);
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