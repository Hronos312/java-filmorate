package ru.yandex.practicum.filmorate.storage.event;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Repository("eventDbStorage")
public class EventDbStorage implements EventStorage {

    private static final String INSERT_EVENT_QUERY = """
        INSERT INTO events (timestamp, user_id, event_type, operation, entity_id)
        VALUES (?, ?, ?, ?, ?)
        """;

    private static final String FIND_FEED_QUERY = """
        SELECT event_id, timestamp, user_id, event_type, operation, entity_id
        FROM events
        WHERE user_id = ?
        ORDER BY timestamp ASC
        """;

    private final JdbcTemplate jdbc;

    public EventDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void addEvent(Event event) {
        jdbc.update(
            INSERT_EVENT_QUERY,
            event.getTimestamp(),
            event.getUserId(),
            event.getEventType().name(),
            event.getOperation().name(),
            event.getEntityId()
        );
    }

    @Override
    public Collection<Event> getFeedByUserId(Long userId) {
        return jdbc.query(FIND_FEED_QUERY, this::mapRow, userId);
    }

    private Event mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return Event.builder()
            .eventId(resultSet.getLong("event_id"))
            .timestamp(resultSet.getLong("timestamp"))
            .userId(resultSet.getLong("user_id"))
            .eventType(EventType.valueOf(resultSet.getString("event_type")))
            .operation(Operation.valueOf(resultSet.getString("operation")))
            .entityId(resultSet.getLong("entity_id"))
            .build();
    }
}
