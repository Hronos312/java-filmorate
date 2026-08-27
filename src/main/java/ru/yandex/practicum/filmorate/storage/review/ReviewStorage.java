package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

public interface ReviewStorage {

    List<Review> findAll(Integer count);

    Review findById(Long id);

    List<Review> findByFilmId(Long filmId, Integer count);

    Review create(Review review);

    Review update(Review review);

    void delete(Long id);

    void addLike(Long reviewId, Long userId);

    void removeLike(Long reviewId, Long userId);

    void addDislike(Long reviewId, Long userId);

    void removeDislike(Long reviewId, Long userId);
}