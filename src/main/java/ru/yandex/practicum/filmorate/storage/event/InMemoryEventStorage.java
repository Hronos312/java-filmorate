package ru.yandex.practicum.filmorate.storage.event;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component("inMemoryEventStorage")
public class InMemoryEventStorage implements EventStorage {

    private final Map<Long, Event> events = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public void addEvent(Event event) {
        long eventId = idGenerator.incrementAndGet();
        event.setEventId(eventId);

        events.put(eventId, event);
    }

    @Override
    public Collection<Event> getFeedByUserId(Long userId) {
        return events.values().stream()
            .filter(event -> event.getUserId().equals(userId))
            .sorted(Comparator.comparingLong(Event::getTimestamp))
            .collect(Collectors.toList());
    }

    public void clear() {
        events.clear();
        idGenerator.set(0);
    }
}
