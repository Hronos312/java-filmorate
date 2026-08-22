package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public Film findById(Long id) {
        Film film = films.get(id);

        if (film == null) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }

        return film;
    }

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {

        if (film.getId() == null) {
            throw new ValidationException("id должен быть указан");
        }
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }

        films.put(film.getId(), film);
        return film;
    }

    @Override
    public void delete(Long id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }

        films.remove(id);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        findById(filmId).getLikes().add(userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        findById(filmId).getLikes().remove(userId);
    }

    @Override
    public Collection<Film> findPopular(Integer count, Long genreId, Integer year) {
        return films.values()
                .stream()
                .filter(film -> genreId == null || film.getGenres().stream()
                        .anyMatch(genre -> genreId.equals(genre.getId())))
                .filter(film -> year == null || year.equals(film.getReleaseDate().getYear()))
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .toList();
    }

    @Override
    public Collection<Film> findByDirector(Long directorId, String sortBy) {
        Comparator<Film> comparator;

        if ("year".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(Film::getReleaseDate);
        } else if ("likes".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed();
        } else {
            throw new ValidationException("Параметр sortBy должен иметь значение year или likes");
        }

        return films.values().stream()
                .filter(film -> film.getDirectors().stream()
                        .anyMatch(director -> director.getId().equals(directorId))
                )
                .sorted(comparator)
                .toList();
    }

    @Override
    public Collection<Film> getRecommendations(Long userId) {
        Map<Long, Set<Long>> userLikesMap = new HashMap<>();
        for (Film film : films.values()) {
            for (Long uId : film.getLikes()) {
                userLikesMap.computeIfAbsent(uId, k -> new HashSet<>()).add(film.getId());
            }
        }

        Set<Long> targetLikes = userLikesMap.getOrDefault(userId, Collections.emptySet());

        Long mostSimilarUserId = null;
        long maxIntersection = 0;

        for (Map.Entry<Long, Set<Long>> entry : userLikesMap.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(userId)) {
                continue;
            }

            Set<Long> otherLikes = entry.getValue();
            long intersectionSize = otherLikes.stream()
                    .filter(targetLikes::contains)
                    .count();

            if (intersectionSize > maxIntersection) {
                maxIntersection = intersectionSize;
                mostSimilarUserId = otherUserId;
            }
        }

        if (mostSimilarUserId == null || maxIntersection == 0) {
            return Collections.emptyList();
        }

        Set<Long> recommendedFilmIds = userLikesMap.get(mostSimilarUserId).stream()
                .filter(filmId -> !targetLikes.contains(filmId))
                .collect(Collectors.toSet());

        return films.values().stream()
                .filter(film -> recommendedFilmIds.contains(film.getId()))
                .toList();
    }
}
