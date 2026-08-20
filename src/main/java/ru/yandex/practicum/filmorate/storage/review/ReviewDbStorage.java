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
import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static final String CHECK_USER_REACTION_QUERY = """
            SELECT is_like FROM review_likes WHERE review_id = ? AND user_id = ?
            """;

    private static final String UPDATE_USEFUL_QUERY = """
            UPDATE reviews SET useful = useful + ? WHERE review_id = ?
            """;

    private static final String UPSERT_REACTION_QUERY = """
            MERGE INTO review_likes (review_id, user_id, is_like)
            KEY (review_id, user_id)
            VALUES (?, ?, ?)
            """;

    private static final String DELETE_REACTION_QUERY = """
            DELETE FROM review_likes WHERE review_id = ? AND user_id = ?
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
            PreparedStatement statement = connection.prepareStatement(INSERT_QUERY, new String[]{"review_id"});
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
        int updatedRows = jdbc.update(UPDATE_QUERY, review.getContent(), review.getIsPositive(), review.getId());
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
    @Transactional
    public void addLike(Long reviewId, Long userId) {
        findById(reviewId);
        Boolean isLike = getUserReaction(reviewId, userId);

        if (Boolean.FALSE.equals(isLike)) {
            jdbc.update(UPDATE_USEFUL_QUERY, 2, reviewId);
        } else if (isLike == null) {
            jdbc.update(UPDATE_USEFUL_QUERY, 1, reviewId);
        }
        jdbc.update(UPSERT_REACTION_QUERY, reviewId, userId, true);
    }

    @Override
    @Transactional
    public void removeLike(Long reviewId, Long userId) {
        findById(reviewId);
        Boolean isLike = getUserReaction(reviewId, userId);

        if (Boolean.TRUE.equals(isLike)) {
            jdbc.update(UPDATE_USEFUL_QUERY, -1, reviewId);
            jdbc.update(DELETE_REACTION_QUERY, reviewId, userId);
        }
    }

    @Override
    @Transactional
    public void addDislike(Long reviewId, Long userId) {
        findById(reviewId);
        Boolean isLike = getUserReaction(reviewId, userId);

        if (Boolean.TRUE.equals(isLike)) {
            jdbc.update(UPDATE_USEFUL_QUERY, -2, reviewId);
        } else if (isLike == null) {
            jdbc.update(UPDATE_USEFUL_QUERY, -1, reviewId);
        }
        jdbc.update(UPSERT_REACTION_QUERY, reviewId, userId, false);
    }

    @Override
    @Transactional
    public void removeDislike(Long reviewId, Long userId) {
        findById(reviewId);
        Boolean isLike = getUserReaction(reviewId, userId);

        if (Boolean.FALSE.equals(isLike)) {
            jdbc.update(UPDATE_USEFUL_QUERY, 1, reviewId);
            jdbc.update(DELETE_REACTION_QUERY, reviewId, userId);
        }
    }

    private Boolean getUserReaction(Long reviewId, Long userId) {
        return jdbc.query(CHECK_USER_REACTION_QUERY,
                        (rs, rowNum) -> rs.getBoolean("is_like"), reviewId, userId)
                .stream().findFirst().orElse(null);
    }

    private Review mapRow(ResultSet resultSet, int rowNum) throws SQLException {
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