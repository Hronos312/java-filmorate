package ru.yandex.practicum.filmorate.storage.review;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class ReviewDbStorage implements ReviewStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT review_id, film_id, user_id, content, is_positive, created, useful
            FROM reviews
            ORDER BY review_id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT review_id, film_id, user_id, content, is_positive, created, useful
            FROM reviews
            WHERE review_id = ?
            """;

    private static final String FIND_BY_FILM_ID_QUERY = """
            SELECT review_id, film_id, user_id, content, is_positive, created, useful
            FROM reviews
            WHERE film_id = ?
            ORDER BY useful DESC, review_id
            """;

    private static final String INSERT_QUERY = """
            INSERT INTO reviews (film_id, user_id, content, is_positive, created, useful)
            VALUES (?, ?, ?, ?, CURRENT_DATE, 0)
            """;

    private static final String UPDATE_QUERY = """
            UPDATE reviews
            SET content = ?, is_positive = ?
            WHERE review_id = ?
            """;

    private static final String DELETE_QUERY = """
            DELETE FROM reviews
            WHERE review_id = ?
            """;

    private static final String ADD_LIKE_QUERY = """
            UPDATE reviews
            SET useful = useful + 1
            WHERE review_id = ?
            """;

    private static final String REMOVE_LIKE_QUERY = """
            UPDATE reviews
            SET useful = useful - 1
            WHERE review_id = ?
            """;

    private final JdbcTemplate jdbc;

    public ReviewDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Review> findAll() {
        return jdbc.query(FIND_ALL_QUERY, this::mapRow);
    }

    @Override
    public Review findById(Long id) {
        List<Review> reviews = jdbc.query(FIND_BY_ID_QUERY, this::mapRow, id);

        return reviews.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Отзыв с id " + id + " не найден"));
    }

    @Override
    public List<Review> findByFilmId(Long filmId) {
        return jdbc.query(FIND_BY_FILM_ID_QUERY, this::mapRow, filmId);
    }

    @Override
    @Transactional
    public Review create(Review review) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);

            statement.setLong(1, review.getFilmId());
            statement.setLong(2, review.getUserId());
            statement.setString(3, review.getContent());
            statement.setBoolean(4, review.getIsPositive());

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Не удалось получить id созданного отзыва");
        }

        review.setId(generatedId.longValue());

        return findById(review.getId());
    }

    @Override
    @Transactional
    public Review update(Review review) {
        if (review.getId() == null) {
            throw new ValidationException("id должен быть указан");
        }

        findById(review.getId());

        int updatedRows = jdbc.update(
                UPDATE_QUERY,
                review.getContent(),
                review.getIsPositive(),
                review.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Отзыв с id " + review.getId() + " не найден");
        }

        return findById(review.getId());
    }

    @Override
    public void delete(Long id) {
        int deletedRows = jdbc.update(DELETE_QUERY, id);

        if (deletedRows == 0) {
            throw new NotFoundException("Отзыв с id " + id + " не найден");
        }
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        findById(reviewId);
        jdbc.update(ADD_LIKE_QUERY, reviewId);
    }

    @Override
    public void removeLike(Long reviewId, Long userId) {
        findById(reviewId);
        jdbc.update(REMOVE_LIKE_QUERY, reviewId);
    }

    private Review mapRow(java.sql.ResultSet resultSet, int rowNum)
            throws java.sql.SQLException {

        Review review = new Review();

        review.setId(resultSet.getLong("review_id"));
        review.setFilmId(resultSet.getLong("film_id"));
        review.setUserId(resultSet.getLong("user_id"));
        review.setContent(resultSet.getString("content"));
        review.setIsPositive(resultSet.getBoolean("is_positive"));
        review.setCreated(resultSet.getDate("created").toLocalDate());
        review.setUseful(resultSet.getInt("useful"));

        return review;
    }
}