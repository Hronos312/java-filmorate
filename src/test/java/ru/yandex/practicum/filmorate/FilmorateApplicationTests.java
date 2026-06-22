package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {

	@Test
	void contextLoads() {
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
	void shouldThrowExceptionWhenFilmNameIsBlank() {
		FilmController filmController = new FilmController();
		Film film = makeValidFilm();
		film.setName("");

		assertThrows(ValidationException.class, () -> filmController.create(film));
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
	void shouldThrowExceptionWhenFilmDescriptionLengthMoreThan200() {
		FilmController filmController = new FilmController();
		Film film = makeValidFilm();
		film.setDescription("a".repeat(201));

		assertThrows(ValidationException.class, () -> filmController.create(film));
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
	void shouldThrowExceptionWhenFilmDurationIsZero() {
		FilmController filmController = new FilmController();
		Film film = makeValidFilm();
		film.setDuration(0);

		assertThrows(ValidationException.class, () -> filmController.create(film));
	}

	@Test
	void shouldThrowExceptionWhenFilmDurationIsNegative() {
		FilmController filmController = new FilmController();
		Film film = makeValidFilm();
		film.setDuration(-1);

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
	void shouldThrowExceptionWhenUserEmailIsBlank() {
		UserController userController = new UserController();
		User user = makeValidUser();
		user.setEmail("");

		assertThrows(ValidationException.class, () -> userController.create(user));
	}

	@Test
	void shouldThrowExceptionWhenUserEmailDoesNotContainAt() {
		UserController userController = new UserController();
		User user = makeValidUser();
		user.setEmail("ivanmail.ru");

		assertThrows(ValidationException.class, () -> userController.create(user));
	}

	@Test
	void shouldThrowExceptionWhenUserLoginIsBlank() {
		UserController userController = new UserController();
		User user = makeValidUser();
		user.setLogin("");

		assertThrows(ValidationException.class, () -> userController.create(user));
	}

	@Test
	void shouldThrowExceptionWhenUserLoginContainsSpace() {
		UserController userController = new UserController();
		User user = makeValidUser();
		user.setLogin("ivan login");

		assertThrows(ValidationException.class, () -> userController.create(user));
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
	void shouldThrowExceptionWhenUserBirthdayIsInFuture() {
		UserController userController = new UserController();
		User user = makeValidUser();
		user.setBirthday(LocalDate.now().plusDays(1));

		assertThrows(ValidationException.class, () -> userController.create(user));
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