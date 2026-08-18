package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

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

    private Genre mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(resultSet.getLong("genre_id"));
        genre.setName(resultSet.getString("name"));

        return genre;
    }
}