package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final EventStorage eventStorage;

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                        @Qualifier("userDbStorage") UserStorage userStorage,
                        @Qualifier("eventDbStorage") EventStorage eventStorage
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.eventStorage = eventStorage;
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(Long id) {
        return filmStorage.findById(id);
    }

    public Film create(Film film) {
        validateReleaseDate(film);

        Film createdFilm = filmStorage.create(film);

        log.info("Добавлен фильм: {}", createdFilm);

        return createdFilm;
    }

    public Film update(Film film) {
        validateReleaseDate(film);

        Film updatedFilm = filmStorage.update(film);

        log.info("Обновлён фильм: {}", updatedFilm);

        return updatedFilm;
    }

    public void addLike(Long filmId, Long userId) {
        filmStorage.findById(filmId);
        userStorage.findById(userId);

        filmStorage.addLike(filmId, userId);

        eventStorage.addEvent(Event.builder()
            .timestamp(Instant.now().toEpochMilli())
            .userId(userId)
            .eventType(EventType.LIKE)
            .operation(Operation.ADD)
            .entityId(filmId)
            .build());

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        filmStorage.findById(filmId);
        userStorage.findById(userId);

        filmStorage.removeLike(filmId, userId);

        eventStorage.addEvent(Event.builder()
            .timestamp(Instant.now().toEpochMilli())
            .userId(userId)
            .eventType(EventType.LIKE)
            .operation(Operation.REMOVE)
            .entityId(filmId)
            .build());

        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public Collection<Film> getPopularFilms(Integer count) {
        if (count == null) {
            count = 10;
        }

        if (count <= 0) {
            log.warn("Ошибка валидации: некорректный параметр count {}", count);

            throw new ValidationException("Параметр count должен быть положительным");
        }

        return filmStorage.findPopular(count);
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза {}", film.getReleaseDate());
            throw new ValidationException("Дата релиза должна быть не ранее 28.12.1895");
        }
    }

}
