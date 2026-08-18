package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class GenreDbStorage implements GenreStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT genre_id, name
            FROM genres
            ORDER BY genre_id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT genre_id, name
            FROM genres
            WHERE genre_id = ?
            """;

    private static final String FIND_BY_FILMS = """
            SELECT fg.film_id, g.genre_id, g.name
            FROM film_genres AS fg
            JOIN genres AS g ON fg.genre_id = g.genre_id
            WHERE fg.film_id IN (%s)
            ORDER BY g.genre_id
            """;

    private final JdbcTemplate jdbc;

    public GenreDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Collection<Genre> findAll() {
        return jdbc.query(FIND_ALL_QUERY, this::mapRow);
    }

    @Override
    public Genre findById(Long id) {
        List<Genre> genres = jdbc.query(FIND_BY_ID_QUERY, this::mapRow, id);

        return genres.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Жанр с id " + id + " не найден"));
    }

    @Override
    public void loadGenresForFilms(List<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmMap = films.stream()
                .collect(Collectors.toMap(Film::getId, film -> film));

        String inSql = String.join(",", Collections.nCopies(films.size(), "?"));
        String query = String.format(FIND_BY_FILMS, inSql);

        Object[] filmIds = films.stream().map(Film::getId).toArray();

        jdbc.query(query, (rs, rowNum) -> {
            Long filmId = rs.getLong("film_id");
            Film film = filmMap.get(filmId);

            if (film != null) {
                Genre genre = mapRow(rs, rowNum);
                film.getGenres().add(genre);
            }
            return null;
        }, filmIds);
    }

    private Genre mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(resultSet.getLong("genre_id"));
        genre.setName(resultSet.getString("name"));

        return genre;
    }
}