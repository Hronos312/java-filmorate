package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

@Repository
public class   MpaDbStorage implements MpaStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT mpa_id, name
            FROM mpa
            ORDER BY mpa_id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT mpa_id, name
            FROM mpa
            WHERE mpa_id = ?
            """;

    private final JdbcTemplate jdbc;

    public MpaDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Collection<Mpa> findAll() {
        return jdbc.query(FIND_ALL_QUERY, this::mapRow);
    }

    @Override
    public Mpa findById(Long id) {
        List<Mpa> ratings = jdbc.query(FIND_BY_ID_QUERY, this::mapRow, id);

        return ratings.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id " + id + " не найден"));
    }

    private Mpa mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Mpa mpa = new Mpa();
        mpa.setId(resultSet.getLong("mpa_id"));
        mpa.setName(resultSet.getString("name"));

        return mpa;
    }
}
