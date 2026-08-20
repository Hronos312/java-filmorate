package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Review {

    @JsonProperty("reviewId")
    private Long id;

    @NotNull(message = "ID фильма должен быть указан")
    private Long filmId;

    @NotNull(message = "ID пользователя должен быть указан")
    private Long userId;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    @Size(max = 500, message = "Текст отзыва должен быть до 500 символов")
    private String content;

    @NotNull(message = "Тип отзыва должен быть указан")
    private Boolean isPositive;

    private LocalDate created;

    private Integer useful;
}
