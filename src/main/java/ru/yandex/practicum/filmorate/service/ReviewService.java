package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final EventStorage eventStorage;

    public ReviewService(
            @Qualifier("reviewDbStorage") ReviewStorage reviewStorage,
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage,
            @Qualifier("eventDbStorage") EventStorage eventStorage) {
        this.reviewStorage = reviewStorage;
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.eventStorage = eventStorage;
    }

    public List<Review> findAll(Integer count) {
        log.debug("Получение всех отзывов с ограничением count={}", count);
        if (count == null || count <= 0) {
            count = 10;
        }
        return reviewStorage.findAll(count);
    }

    public Review findById(Long id) {
        if (id == null) {
            throw new ValidationException("ID отзыва не может быть null");
        }
        log.debug("Поиск отзыва с id={}", id);
        return reviewStorage.findById(id);
    }

    public List<Review> findByFilmId(Long filmId, Integer count) {
        log.debug("Поиск отзывов для фильма с id={}", filmId);
        if (count == null || count <= 0) {
            count = 10;
        }
        filmStorage.findById(filmId);
        return reviewStorage.findByFilmId(filmId, count);
    }

    public Review create(Review review) {
        log.debug("Создание отзыва: {}", review);
        validateReview(review);
        filmStorage.findById(review.getFilmId());
        userStorage.findById(review.getUserId());

        Review createdReview = reviewStorage.create(review);
        eventStorage.addEvent(Event.builder()
            .timestamp(Instant.now().toEpochMilli())
            .userId(createdReview.getUserId())
            .eventType(EventType.REVIEW)
            .operation(Operation.ADD)
            .entityId(createdReview.getId())
            .build());

        return reviewStorage.create(review);
    }

    public Review update(Review review) {
        log.debug("Обновление отзыва с id={}: {}", review.getId(), review);
        if (review.getId() == null) {
            throw new ValidationException("ID отзыва должен быть указан");
        }
        reviewStorage.findById(review.getId());
        validateReview(review);

        Review updatedReview = reviewStorage.update(review);
        eventStorage.addEvent(Event.builder()
            .timestamp(Instant.now().toEpochMilli())
            .userId(updatedReview.getUserId())
            .eventType(EventType.REVIEW)
            .operation(Operation.UPDATE)
            .entityId(updatedReview.getId())
            .build());

        return reviewStorage.update(review);
    }

    public void delete(Long id) {
        if (id == null) {
            throw new ValidationException("ID отзыва не может быть null");
        }
        log.debug("Удаление отзыва с id={}", id);

        Review reviewToDelete = reviewStorage.findById(id);

        reviewStorage.delete(id);

        eventStorage.addEvent(Event.builder()
            .timestamp(Instant.now().toEpochMilli())
            .userId(reviewToDelete.getUserId())
            .eventType(EventType.REVIEW)
            .operation(Operation.REMOVE)
            .entityId(id)
            .build());
    }

    public void addLike(Long reviewId, Long userId) {
        if (reviewId == null || userId == null) {
            throw new ValidationException("ID отзыва и пользователя не могут быть null");
        }
        log.debug("Добавление лайка к отзыву с id={} от пользователя id={}", reviewId, userId);
        reviewStorage.findById(reviewId);
        userStorage.findById(userId);
        reviewStorage.addLike(reviewId, userId);
    }

    public void removeLike(Long reviewId, Long userId) {
        if (reviewId == null || userId == null) {
            throw new ValidationException("ID отзыва и пользователя не могут быть null");
        }
        log.debug("Удаление лайка с отзыва с id={} от пользователя id={}", reviewId, userId);
        reviewStorage.findById(reviewId);
        userStorage.findById(userId);
        reviewStorage.removeLike(reviewId, userId);
    }

    public void addDislike(Long reviewId, Long userId) {
        if (reviewId == null || userId == null) {
            throw new ValidationException("ID отзыва и пользователя не могут быть null");
        }
        log.debug("Добавление дизлайка к отзыву с id={} от пользователя id={}", reviewId, userId);
        reviewStorage.findById(reviewId);
        userStorage.findById(userId);
        reviewStorage.addDislike(reviewId, userId);
    }

    public void removeDislike(Long reviewId, Long userId) {
        if (reviewId == null || userId == null) {
            throw new ValidationException("ID отзыва и пользователя не могут быть null");
        }
        log.debug("Удаление дизлайка с отзыва с id={} от пользователя id={}", reviewId, userId);
        reviewStorage.findById(reviewId);
        userStorage.findById(userId);
        reviewStorage.removeDislike(reviewId, userId);
    }

    private void validateReview(Review review) {
        if (review.getFilmId() == null) {
            throw new ValidationException("ID фильма должен быть указан");
        }
        if (review.getUserId() == null) {
            throw new ValidationException("ID пользователя должен быть указан");
        }
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ValidationException("Текст отзыва не может быть пустым");
        }
        if (review.getIsPositive() == null) {
            throw new ValidationException("Тип отзыва должен быть указан");
        }
    }
}