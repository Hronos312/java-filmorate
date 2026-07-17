package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class Film {

    private Long id;
    private final Set<Long> likes = new HashSet<>();
    private Set<Genre> genres = new HashSet<>();
    private Mpa mpa;

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;

    @Size(max = 200, message = "Описание должно быть до 200 символов")
    private String description;

    @NotNull(message = "Дата релиза должна быть указана")
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность должна быть указана")
    @Positive(message = "Продолжительность должна быть положительной")
    private Integer duration;

}
