package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
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
        Film film = filmStorage.findById(filmId);
        userStorage.findById(userId);

        film.getLikes().add(userId);

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = filmStorage.findById(filmId);
        userStorage.findById(userId);

        film.getLikes().remove(userId);

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

        return filmStorage.findAll()
                .stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .toList();
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза {}", film.getReleaseDate());
            throw new ValidationException("Дата релиза должна быть не ранее 28.12.1895");
        }
    }

}
