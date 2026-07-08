package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {

    private Long id;
    private Set<Long> friends = new HashSet<>();

    @NotBlank(message = "Электронная почта не может быть пустой")
    @Email(message = "Неверный формат почты")
    private String email;

    @NotBlank(message = "Логин не может быть пустым")
    @Pattern(regexp = "\\S+", message = "Логин не должен содержать пробелы")
    private String login;

    private String name;

    @NotNull(message = "Дата рождения должна быть указана")
    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;

}
