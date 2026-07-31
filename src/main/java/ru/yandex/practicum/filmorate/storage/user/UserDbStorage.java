package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository("userDbStorage")
public class UserDbStorage implements UserStorage {

    private static final String FIND_ALL_QUERY = """
            SELECT user_id, email, login, name, birthday
            FROM users
            ORDER BY user_id
            """;

    private static final String FIND_BY_ID_QUERY = """
            SELECT user_id, email, login, name, birthday
            FROM users
            WHERE user_id = ?
            """;

    private static final String INSERT_QUERY = """
            INSERT INTO users (email, login, name, birthday)
            VALUES (?, ?, ?, ?)
            """;

    private static final String UPDATE_QUERY = """
            UPDATE users
            SET email = ?,
                login = ?,
                name = ?,
                birthday = ?
            WHERE user_id = ?
            """;

    private static final String DELETE_QUERY = """
            DELETE FROM users
            WHERE user_id = ?
            """;

    private static final String FIND_FRIENDS_QUERY = """
            SELECT f.friend_id,
                   fs.name AS status_name
            FROM friendships AS f
            JOIN friendship_statuses AS fs
              ON fs.status_id = f.status_id
            WHERE f.user_id = ?
            ORDER BY f.friend_id
            """;

    private static final String FIND_STATUS_ID_QUERY = """
            SELECT status_id
            FROM friendship_statuses
            WHERE name = ?
            """;

    private static final String CHECK_FRIENDSHIP_QUERY = """
            SELECT COUNT(*)
            FROM friendships
            WHERE user_id = ?
            AND friend_id = ?
            """;

    private static final String SAVE_FRIENDSHIP_QUERY = """
            MERGE INTO friendships (user_id, friend_id, status_id)
            KEY (user_id, friend_id)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE_FRIENDSHIP_STATUS_QUERY = """
            UPDATE friendships
            SET status_id = ?
            WHERE user_id = ?
              AND friend_id = ?
            """;

    private static final String REMOVE_FRIENDSHIP_QUERY = """
            DELETE FROM friendships
            WHERE user_id = ?
              AND friend_id = ?
            """;

    private static final String FIND_FRIEND_USERS_QUERY = """
            SELECT u.user_id,
                   u.email,
                   u.login,
                   u.name,
                   u.birthday
            FROM friendships AS f
            JOIN users AS u ON u.user_id = f.friend_id
            WHERE f.user_id = ?
            ORDER BY u.user_id
            """;

    private static final String FIND_COMMON_FRIENDS_QUERY = """
            SELECT u.user_id,
                   u.email,
                   u.login,
                   u.name,
                   u.birthday
            FROM friendships AS first_friendship
            JOIN friendships AS second_friendship
              ON first_friendship.friend_id =
                 second_friendship.friend_id
            JOIN users AS u
              ON u.user_id = first_friendship.friend_id
            WHERE first_friendship.user_id = ?
              AND second_friendship.user_id = ?
            ORDER BY u.user_id
            """;

    private final JdbcTemplate jdbc;

    public UserDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Collection<User> findAll() {
        List<User> users = jdbc.query(FIND_ALL_QUERY, this::mapRow);
        users.forEach(this::loadFriends);

        return users;
    }

    @Override
    public User findById(Long id) {
        List<User> users = jdbc.query(FIND_BY_ID_QUERY, this::mapRow, id);

        User user = users.stream()
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id = " + id + " не найден"));

        loadFriends(user);

        return user;
    }

    @Override
    public User create(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    INSERT_QUERY,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());
            statement.setDate(4, Date.valueOf(user.getBirthday()));

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Не удалось получить id созданного пользователя");
        }

        user.setId(generatedId.longValue());

        return user;
    }

    @Override
    public User update(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        int updatedRows = jdbc.update(
                UPDATE_QUERY,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }

        return findById(user.getId());
    }

    @Override
    public void delete(Long id) {
        int deletedRows = jdbc.update(DELETE_QUERY, id);

        if (deletedRows == 0) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
    }

    private User mapRow(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        User user = new User();

        user.setId(resultSet.getLong("user_id"));
        user.setEmail(resultSet.getString("email"));
        user.setLogin(resultSet.getString("login"));
        user.setName(resultSet.getString("name"));
        user.setBirthday(
                resultSet.getDate("birthday").toLocalDate()
        );

        return user;
    }

    @Override
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        boolean reverseFriendshipExists =
                friendshipExists(friendId, userId);

        FriendshipStatus newStatus =
                reverseFriendshipExists
                        ? FriendshipStatus.CONFIRMED
                        : FriendshipStatus.UNCONFIRMED;

        int newStatusId = findStatusId(newStatus);

        jdbc.update(
                SAVE_FRIENDSHIP_QUERY,
                userId,
                friendId,
                newStatusId
        );

        if (reverseFriendshipExists) {
            int confirmedStatusId =
                    findStatusId(FriendshipStatus.CONFIRMED);

            jdbc.update(
                    UPDATE_FRIENDSHIP_STATUS_QUERY,
                    confirmedStatusId,
                    friendId,
                    userId
            );
        }
    }

    @Override
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        jdbc.update(
                REMOVE_FRIENDSHIP_QUERY,
                userId,
                friendId
        );

        if (friendshipExists(friendId, userId)) {
            int unconfirmedStatusId =
                    findStatusId(FriendshipStatus.UNCONFIRMED);

            jdbc.update(
                    UPDATE_FRIENDSHIP_STATUS_QUERY,
                    unconfirmedStatusId,
                    friendId,
                    userId
            );
        }
    }

    @Override
    public Collection<User> getFriends(Long userId) {
        List<User> friends = jdbc.query(
                FIND_FRIEND_USERS_QUERY,
                this::mapRow,
                userId
        );

        friends.forEach(this::loadFriends);

        return friends;
    }

    @Override
    public Collection<User> getCommonFriends(
            Long userId,
            Long otherId
    ) {
        List<User> commonFriends = jdbc.query(
                FIND_COMMON_FRIENDS_QUERY,
                this::mapRow,
                userId,
                otherId
        );

        commonFriends.forEach(this::loadFriends);

        return commonFriends;
    }

    private boolean friendshipExists(Long userId, Long friendId) {
        Integer count = jdbc.queryForObject(
                CHECK_FRIENDSHIP_QUERY,
                Integer.class,
                userId,
                friendId
        );

        return count != null && count > 0;
    }

    private int findStatusId(FriendshipStatus status) {
        Integer statusId = jdbc.queryForObject(
                FIND_STATUS_ID_QUERY,
                Integer.class,
                status.name()
        );

        if (statusId == null) {
            throw new IllegalStateException(
                    "Статус дружбы " + status + " не найден"
            );
        }

        return statusId;
    }

    private void loadFriends(User user) {
        List<Map.Entry<Long, FriendshipStatus>> friendships = jdbc.query(
                FIND_FRIENDS_QUERY,
                (resultSet, rowNum) -> Map.entry(
                        resultSet.getLong("friend_id"),
                        FriendshipStatus.valueOf(
                                resultSet.getString("status_name")
                        )
                ),
                user.getId()
        );

        user.getFriends().clear();

        for (Map.Entry<Long, FriendshipStatus> friendship : friendships) {
            user.getFriends().put(
                    friendship.getKey(),
                    friendship.getValue()
            );
        }
    }
}