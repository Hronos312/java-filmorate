package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public User findById(Long id) {
        User user = users.get(id);

        if (user == null) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }

        return user;
    }

    @Override
    public User create(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);

        return user;
    }

    @Override
    public User update(User user) {

        if (user.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(user.getId())) {
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }

        users.put(user.getId(), user);

        return user;
    }

    @Override
    public void delete(Long id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }

        users.remove(id);
    }
}
