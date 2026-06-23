package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();

    @GetMapping
    public Collection<Film> findAll() {
        return films.values();
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {

        validateReleaseDate(film);

        film.setId(getNextId());
        films.put(film.getId(), film);

        log.info("Добавлен фильм: {}", film);

        return film;

    }

    @PutMapping
    public Film update(@Valid @RequestBody Film newFilm) {

        if (newFilm.getId() == null) {
            log.warn("Ошибка валидации фильма: id не указан");
            throw new ValidationException("id должен быть указан");
        }

        if (!films.containsKey(newFilm.getId())) {
            log.warn("Фильм с id {} не найден", newFilm.getId());
            throw new NotFoundException("Фильм с id " + newFilm.getId() + " не найден");
        }

        validateReleaseDate(newFilm);

        films.put(newFilm.getId(), newFilm);

        log.info("Обновлён фильм: {}", newFilm);
        return newFilm;
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза");
            throw new ValidationException("Дата релиза должна быть не ранее 28.12.1895");
        }
    }

}
