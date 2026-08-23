package ru.yandex.practicum.filmorate.storage.director;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;


@Repository("directorDbStorage")
public class DirectorDbStorage implements DirectorStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT director_id, name
            FROM directors
            ORDER BY director_id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT director_id, name
            FROM directors
            WHERE director_id = ?
            """;

    private static final String INSERT_QUERY = """
            INSERT INTO directors (name)
            VALUES (?)
            """;

    private static final String UPDATE_QUERY = """
            UPDATE directors
            SET name = ?
            WHERE director_id = ?
            """;

    private static final String DELETE_QUERY = """
            DELETE FROM directors
            WHERE director_id = ?
            """;

    private final JdbcTemplate jdbc;

    public DirectorDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Collection<Director> findAll() {
        return jdbc.query(FIND_ALL_QUERY, this::mapRow);
    }

    @Override
    public Director findById(Long id) {
        List<Director> directors = jdbc.query(
                FIND_BY_ID_QUERY,
                this::mapRow,
                id
        );

        return directors.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Режиссёр с id " + id + " не найден"));
    }

    @Override
    public Director create(Director director) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    INSERT_QUERY,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, director.getName());
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Не удалось получить id созданного режиссёра");
        }

        director.setId(generatedId.longValue());

        return director;
    }

    @Override
    public Director update(Director director) {
        if (director.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        int updatedRows = jdbc.update(
                UPDATE_QUERY,
                director.getName(),
                director.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Режиссёр с id " + director.getId() + " не найден");
        }

        return findById(director.getId());
    }

    @Override
    public void delete(Long id) {
        int deletedRows = jdbc.update(
                DELETE_QUERY,
                id
        );

        if (deletedRows == 0) {
            throw new NotFoundException(
                    "Режиссёр с id " + id + " не найден"
            );
        }
    }



    private Director mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Director director = new Director();

        director.setId(resultSet.getLong("director_id"));
        director.setName(resultSet.getString("name"));

        return director;
    }
}
