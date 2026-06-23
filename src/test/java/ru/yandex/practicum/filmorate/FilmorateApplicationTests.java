package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {

	private final Validator validator;

	FilmorateApplicationTests() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void shouldPassValidationWhenUserIsValid() {
		User user = makeValidUser();

		Set<ConstraintViolation<User>> violations = validator.validate(user);

		assertTrue(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenUserEmailIsInvalid() {
		User user = makeValidUser();
		user.setEmail("это-неправильный?эмейл@.");

		Set<ConstraintViolation<User>> violations = validator.validate(user);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenUserEmailIsBlank() {
		User user = makeValidUser();
		user.setEmail("");

		Set<ConstraintViolation<User>> violations = validator.validate(user);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenUserLoginIsBlank() {
		User user = makeValidUser();
		user.setLogin("");

		Set<ConstraintViolation<User>> violations = validator.validate(user);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenUserLoginContainsSpace() {
		User user = makeValidUser();
		user.setLogin("ivan login");

		Set<ConstraintViolation<User>> violations = validator.validate(user);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenUserBirthdayIsNull() {
		User user = makeValidUser();
		user.setBirthday(null);

		Set<ConstraintViolation<User>> violations = validator.validate(user);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenUserBirthdayIsInFuture() {
		User user = makeValidUser();
		user.setBirthday(LocalDate.now().plusDays(1));

		Set<ConstraintViolation<User>> violations = validator.validate(user);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldPassValidationWhenFilmIsValid() {
		Film film = makeValidFilm();

		Set<ConstraintViolation<Film>> violations = validator.validate(film);

		assertTrue(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenFilmNameIsBlank() {
		Film film = makeValidFilm();
		film.setName("");

		Set<ConstraintViolation<Film>> violations = validator.validate(film);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenFilmDescriptionIsTooLong() {
		Film film = makeValidFilm();
		film.setDescription("a".repeat(201));

		Set<ConstraintViolation<Film>> violations = validator.validate(film);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldPassValidationWhenFilmDescriptionLengthIs200() {
		Film film = makeValidFilm();
		film.setDescription("a".repeat(200));

		Set<ConstraintViolation<Film>> violations = validator.validate(film);

		assertTrue(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenFilmDurationIsNegative() {
		Film film = makeValidFilm();
		film.setDuration(-1);

		Set<ConstraintViolation<Film>> violations = validator.validate(film);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenFilmDurationIsNull() {
		Film film = makeValidFilm();
		film.setDuration(null);

		Set<ConstraintViolation<Film>> violations = validator.validate(film);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldNotPassValidationWhenFilmReleaseDateIsNull() {
		Film film = makeValidFilm();
		film.setReleaseDate(null);

		Set<ConstraintViolation<Film>> violations = validator.validate(film);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldCreateFilmWithValidData() {
		FilmController filmController = new FilmController();
		Film film = makeValidFilm();

		Film createdFilm = filmController.create(film);

		assertNotNull(createdFilm.getId());
		assertEquals("Avatar", createdFilm.getName());
	}

	@Test
	void shouldCreateFilmWhenDescriptionLengthIs200() {
		FilmController filmController = new FilmController();
		Film film = makeValidFilm();
		film.setDescription("a".repeat(200));

		Film createdFilm = filmController.create(film);

		assertNotNull(createdFilm.getId());
	}

	@Test
	void shouldCreateFilmWhenReleaseDateIsBoundaryDate() {
		FilmController filmController = new FilmController();
		Film film = makeValidFilm();
		film.setReleaseDate(LocalDate.of(1895, 12, 28));

		Film createdFilm = filmController.create(film);

		assertNotNull(createdFilm.getId());
	}

	@Test
	void shouldThrowExceptionWhenFilmReleaseDateBeforeBoundaryDate() {
		FilmController filmController = new FilmController();
		Film film = makeValidFilm();
		film.setReleaseDate(LocalDate.of(1895, 12, 27));

		assertThrows(ValidationException.class, () -> filmController.create(film));
	}

	@Test
	void shouldCreateUserWithValidData() {
		UserController userController = new UserController();
		User user = makeValidUser();

		User createdUser = userController.create(user);

		assertNotNull(createdUser.getId());
		assertEquals("ivan", createdUser.getLogin());
	}

	@Test
	void shouldSetLoginAsNameWhenUserNameIsBlank() {
		UserController userController = new UserController();
		User user = makeValidUser();
		user.setName("");

		User createdUser = userController.create(user);

		assertEquals(user.getLogin(), createdUser.getName());
	}

	@Test
	void shouldCreateUserWhenBirthdayIsToday() {
		UserController userController = new UserController();
		User user = makeValidUser();
		user.setBirthday(LocalDate.now());

		User createdUser = userController.create(user);

		assertNotNull(createdUser.getId());
	}

	private Film makeValidFilm() {
		Film film = new Film();
		film.setName("Avatar");
		film.setDescription("Good film");
		film.setReleaseDate(LocalDate.of(2009, 12, 10));
		film.setDuration(162);
		return film;
	}

	private User makeValidUser() {
		User user = new User();
		user.setEmail("ivan@mail.ru");
		user.setLogin("ivan");
		user.setName("Ivan");
		user.setBirthday(LocalDate.of(2000, 1, 1));
		return user;
	}
}