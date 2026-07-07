package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
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

    public void addFriend(Long userId, Long friendId) {
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
    }

    public Collection<User> getFriends(Long userId) {
        User user = userStorage.findById(userId);

        return user.getFriends()
                .stream()
                .map(userStorage::findById)
                .toList();
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        User user = userStorage.findById(userId);
        User otherUser = userStorage.findById(otherId);

        return user.getFriends()
                .stream()
                .filter(otherUser.getFriends()::contains)
                .map(userStorage::findById)
                .toList();
    }

    private void setNameIfEmpty(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

}
