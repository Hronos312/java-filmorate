package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final DirectorStorage directorStorage;

    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage,
            @Qualifier("directorDbStorage") DirectorStorage directorStorage
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.directorStorage = directorStorage;
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

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        filmStorage.findById(filmId);
        userStorage.findById(userId);

        filmStorage.removeLike(filmId, userId);

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

    public Collection<Film> getFilmsByDirector(Long directorId, String sortBy) {
        directorStorage.findById(directorId);

        if (!"year".equalsIgnoreCase(sortBy) && !"likes".equalsIgnoreCase(sortBy)) {
            throw new ValidationException("Параметр sortBy должен иметь значение year или likes");
        }

        return filmStorage.findByDirector(directorId, sortBy);
    }

    public void delete(Long id) {
        filmStorage.delete(id);

        log.info("Фильм с id {} удалён", id);
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза {}", film.getReleaseDate());
            throw new ValidationException("Дата релиза должна быть не ранее 28.12.1895");
        }
    }

    public Collection<Film> search(String query, String by) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Параметр query не может быть пустым");
        }

        if (by == null || by.isBlank()) {
            throw new ValidationException("Параметр by не может быть пустым");
        }

        List<String> searchBy = Arrays.asList(by.split(","));

        for (String param : searchBy) {
            if(!"director".equalsIgnoreCase(param) && !"title".equalsIgnoreCase(param)) {
                throw new ValidationException("Параметр by не может содержать только значения: director, title");
            }
        }
        return filmStorage.search(query, searchBy);
    }

}
