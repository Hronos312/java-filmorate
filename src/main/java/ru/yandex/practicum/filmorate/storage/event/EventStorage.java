package ru.yandex.practicum.filmorate.storage.event;

import ru.yandex.practicum.filmorate.model.Event;
import java.util.Collection;

public interface EventStorage {
    void addEvent(Event event);

    Collection<Event> getFeedByUserId(Long userId);
}
