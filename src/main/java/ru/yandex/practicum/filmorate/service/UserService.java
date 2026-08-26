package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;
    private final EventStorage eventStorage;
    private final FilmStorage filmStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("eventDbStorage") EventStorage eventStorage,
                       @Qualifier("filmDbStorage") FilmStorage filmStorage) {
        this.userStorage = userStorage;
        this.eventStorage = eventStorage;
        this.filmStorage = filmStorage;
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(Long id) {
        return userStorage.findById(id);
    }

    public User create(User user) {
        setNameIfEmpty(user);

        User createdUser = userStorage.create(user);

        log.info("Добавлен пользователь: {}", createdUser);

        return createdUser;
    }

    public User update(User user) {
        setNameIfEmpty(user);

        User updatedUser = userStorage.update(user);

        log.info("Обновлён пользователь: {}", updatedUser);

        return updatedUser;
    }

    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может добавить себя в друзья");
        }

        userStorage.findById(userId);
        userStorage.findById(friendId);

        userStorage.addFriend(userId, friendId);

        eventStorage.addEvent(Event.builder()
            .timestamp(Instant.now().toEpochMilli())
            .userId(userId)
            .eventType(EventType.FRIEND)
            .operation(Operation.ADD)
            .entityId(friendId)
            .build());

        log.info("Пользователь {} добавил пользователя {} в друзья", userId, friendId);
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        userStorage.findById(userId);
        userStorage.findById(friendId);

        userStorage.removeFriend(userId, friendId);

        eventStorage.addEvent(Event.builder()
            .timestamp(Instant.now().toEpochMilli())
            .userId(userId)
            .eventType(EventType.FRIEND)
            .operation(Operation.REMOVE)
            .entityId(friendId)
            .build());

        log.info("Пользователь {} удалил пользователя {} из друзей", userId, friendId);
    }

    public Collection<User> getFriends(Long userId) {
        userStorage.findById(userId);

        return userStorage.getFriends(userId);
    }


    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        userStorage.findById(userId);
        userStorage.findById(otherId);

        return userStorage.getCommonFriends(userId, otherId);
    }

    public Collection<Event> getFeed(Long id) {
        userStorage.findById(id);

        return eventStorage.getFeedByUserId(id);
    }

    public Collection<Film> getRecommendations(Long userId) {
        userStorage.findById(userId);

        Collection<Film> recommendedFilms = filmStorage.getRecommendations(userId);

        log.info("Сформировано {} рекомендаций для пользователя с id: {}", recommendedFilms.size(), userId);
        return recommendedFilms;
    }

    public void delete(Long id) {
        userStorage.delete(id);

        log.info("Пользователь с id {} удалён", id);
    }

    private void setNameIfEmpty(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

}
