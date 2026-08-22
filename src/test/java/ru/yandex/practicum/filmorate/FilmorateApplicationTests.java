package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({UserDbStorage.class, FilmDbStorage.class, GenreDbStorage.class, MpaDbStorage.class})
class FilmorateApplicationTests {

	private static final Validator VALIDATOR =
			Validation.buildDefaultValidatorFactory().getValidator();

	private final UserStorage userStorage;
	private final FilmStorage filmStorage;
	private final GenreStorage genreStorage;
	private final MpaStorage mpaStorage;

	@Autowired
	FilmorateApplicationTests(
			@Qualifier("userDbStorage") UserStorage userStorage,
			@Qualifier("filmDbStorage") FilmStorage filmStorage,
			GenreStorage genreStorage,
			MpaStorage mpaStorage
	) {
		this.userStorage = userStorage;
		this.filmStorage = filmStorage;
		this.genreStorage = genreStorage;
		this.mpaStorage = mpaStorage;
	}

	@Test
	void shouldPassValidationWhenUserIsValid() {
		User user = makeValidUser();

		Set<ConstraintViolation<User>> violations =
				VALIDATOR.validate(user);

		assertThat(violations).isEmpty();
	}

	@Test
	void shouldNotPassValidationWhenUserEmailIsInvalid() {
		User user = makeValidUser();
		user.setEmail("это-неправильный?эмейл@.");

		Set<ConstraintViolation<User>> violations =
				VALIDATOR.validate(user);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldNotPassValidationWhenUserEmailIsBlank() {
		User user = makeValidUser();
		user.setEmail("");

		Set<ConstraintViolation<User>> violations =
				VALIDATOR.validate(user);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldNotPassValidationWhenUserLoginIsBlank() {
		User user = makeValidUser();
		user.setLogin("");

		Set<ConstraintViolation<User>> violations =
				VALIDATOR.validate(user);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldNotPassValidationWhenUserLoginContainsSpace() {
		User user = makeValidUser();
		user.setLogin("ivan login");

		Set<ConstraintViolation<User>> violations =
				VALIDATOR.validate(user);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldNotPassValidationWhenUserBirthdayIsNull() {
		User user = makeValidUser();
		user.setBirthday(null);

		Set<ConstraintViolation<User>> violations =
				VALIDATOR.validate(user);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldNotPassValidationWhenUserBirthdayIsInFuture() {
		User user = makeValidUser();
		user.setBirthday(LocalDate.now().plusDays(1));

		Set<ConstraintViolation<User>> violations =
				VALIDATOR.validate(user);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldPassValidationWhenFilmIsValid() {
		Film film = makeValidFilm();

		Set<ConstraintViolation<Film>> violations =
				VALIDATOR.validate(film);

		assertThat(violations).isEmpty();
	}

	@Test
	void shouldNotPassValidationWhenFilmNameIsBlank() {
		Film film = makeValidFilm();
		film.setName("");

		Set<ConstraintViolation<Film>> violations =
				VALIDATOR.validate(film);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldNotPassValidationWhenFilmDescriptionIsTooLong() {
		Film film = makeValidFilm();
		film.setDescription("a".repeat(201));

		Set<ConstraintViolation<Film>> violations =
				VALIDATOR.validate(film);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldPassValidationWhenFilmDescriptionLengthIs200() {
		Film film = makeValidFilm();
		film.setDescription("a".repeat(200));

		Set<ConstraintViolation<Film>> violations =
				VALIDATOR.validate(film);

		assertThat(violations).isEmpty();
	}

	@Test
	void shouldNotPassValidationWhenFilmDurationIsNegative() {
		Film film = makeValidFilm();
		film.setDuration(-1);

		Set<ConstraintViolation<Film>> violations =
				VALIDATOR.validate(film);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldNotPassValidationWhenFilmDurationIsNull() {
		Film film = makeValidFilm();
		film.setDuration(null);

		Set<ConstraintViolation<Film>> violations =
				VALIDATOR.validate(film);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldNotPassValidationWhenFilmReleaseDateIsNull() {
		Film film = makeValidFilm();
		film.setReleaseDate(null);

		Set<ConstraintViolation<Film>> violations =
				VALIDATOR.validate(film);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void shouldCreateUserInDatabase() {
		User user = makeValidUser();

		User createdUser = userStorage.create(user);

		assertThat(createdUser.getId()).isNotNull();

		User storedUser = userStorage.findById(createdUser.getId());

		assertThat(storedUser.getId()).isEqualTo(createdUser.getId());
		assertThat(storedUser.getEmail()).isEqualTo("ivan@mail.ru");
		assertThat(storedUser.getLogin()).isEqualTo("ivan");
		assertThat(storedUser.getName()).isEqualTo("Ivan");
		assertThat(storedUser.getBirthday())
				.isEqualTo(LocalDate.of(2000, 1, 1));
	}

	@Test
	void shouldFindUserByIdInDatabase() {
		User createdUser = userStorage.create(makeValidUser());

		User foundUser = userStorage.findById(createdUser.getId());

		assertThat(foundUser.getId()).isEqualTo(createdUser.getId());
		assertThat(foundUser.getEmail())
				.isEqualTo(createdUser.getEmail());
		assertThat(foundUser.getLogin())
				.isEqualTo(createdUser.getLogin());
		assertThat(foundUser.getName())
				.isEqualTo(createdUser.getName());
		assertThat(foundUser.getBirthday())
				.isEqualTo(createdUser.getBirthday());
	}

	@Test
	void shouldFindAllUsersInDatabase() {
		User firstUser = userStorage.create(makeValidUser());

		User secondUser = makeValidUser();
		secondUser.setEmail("petr@mail.ru");
		secondUser.setLogin("petr");
		secondUser.setName("Petr");

		secondUser = userStorage.create(secondUser);

		Collection<User> users = userStorage.findAll();

		assertThat(users)
				.extracting(User::getId)
				.containsExactly(
						firstUser.getId(),
						secondUser.getId()
				);
	}

	@Test
	void shouldUpdateUserInDatabase() {
		User user = userStorage.create(makeValidUser());

		user.setEmail("new-email@mail.ru");
		user.setLogin("new-login");
		user.setName("New name");
		user.setBirthday(LocalDate.of(2001, 2, 3));

		User updatedUser = userStorage.update(user);

		assertThat(updatedUser.getId()).isEqualTo(user.getId());
		assertThat(updatedUser.getEmail())
				.isEqualTo("new-email@mail.ru");
		assertThat(updatedUser.getLogin()).isEqualTo("new-login");
		assertThat(updatedUser.getName()).isEqualTo("New name");
		assertThat(updatedUser.getBirthday())
				.isEqualTo(LocalDate.of(2001, 2, 3));

		User storedUser = userStorage.findById(user.getId());

		assertThat(storedUser.getEmail())
				.isEqualTo("new-email@mail.ru");
	}

	@Test
	void shouldDeleteUserFromDatabase() {
		User user = userStorage.create(makeValidUser());
		Long userId = user.getId();

		userStorage.delete(userId);

		assertThatThrownBy(() -> userStorage.findById(userId))
				.isInstanceOf(NotFoundException.class)
				.hasMessage(
						"Пользователь с id = " + userId + " не найден"
				);
	}

	@Test
	void shouldCreateFilmInDatabase() {
		Film film = makeValidFilm();

		Film createdFilm = filmStorage.create(film);

		assertThat(createdFilm.getId()).isNotNull();
		assertThat(createdFilm.getName()).isEqualTo("Avatar");
		assertThat(createdFilm.getDescription())
				.isEqualTo("Good film");
		assertThat(createdFilm.getReleaseDate())
				.isEqualTo(LocalDate.of(2009, 12, 10));
		assertThat(createdFilm.getDuration()).isEqualTo(162);

		assertThat(createdFilm.getMpa().getId()).isEqualTo(3L);
		assertThat(createdFilm.getMpa().getName())
				.isEqualTo("PG-13");

		assertThat(createdFilm.getGenres())
				.extracting(Genre::getId)
				.containsExactly(2L, 4L);
	}

	@Test
	void shouldFindFilmByIdInDatabase() {
		Film createdFilm = filmStorage.create(makeValidFilm());

		Film foundFilm = filmStorage.findById(createdFilm.getId());

		assertThat(foundFilm.getId()).isEqualTo(createdFilm.getId());
		assertThat(foundFilm.getName())
				.isEqualTo(createdFilm.getName());
		assertThat(foundFilm.getDescription())
				.isEqualTo(createdFilm.getDescription());
		assertThat(foundFilm.getReleaseDate())
				.isEqualTo(createdFilm.getReleaseDate());
		assertThat(foundFilm.getDuration())
				.isEqualTo(createdFilm.getDuration());
		assertThat(foundFilm.getMpa().getId()).isEqualTo(3L);

		assertThat(foundFilm.getGenres())
				.extracting(Genre::getId)
				.containsExactly(2L, 4L);
	}

	@Test
	void shouldFindAllFilmsInDatabase() {
		Film firstFilm = filmStorage.create(makeValidFilm());

		Film secondFilm = makeValidFilm();
		secondFilm.setName("Matrix");
		secondFilm.setReleaseDate(LocalDate.of(1999, 3, 31));
		secondFilm.setDuration(136);

		secondFilm = filmStorage.create(secondFilm);

		Collection<Film> films = filmStorage.findAll();

		assertThat(films)
				.extracting(Film::getId)
				.containsExactly(
						firstFilm.getId(),
						secondFilm.getId()
				);
	}

	@Test
	void shouldUpdateFilmInDatabase() {
		Film film = filmStorage.create(makeValidFilm());

		film.setName("New film name");
		film.setDescription("New description");
		film.setReleaseDate(LocalDate.of(2010, 1, 1));
		film.setDuration(180);
		film.setMpa(makeMpa(4L));
		film.setGenres(makeGenres(1L, 6L));

		Film updatedFilm = filmStorage.update(film);

		assertThat(updatedFilm.getId()).isEqualTo(film.getId());
		assertThat(updatedFilm.getName())
				.isEqualTo("New film name");
		assertThat(updatedFilm.getDescription())
				.isEqualTo("New description");
		assertThat(updatedFilm.getReleaseDate())
				.isEqualTo(LocalDate.of(2010, 1, 1));
		assertThat(updatedFilm.getDuration()).isEqualTo(180);

		assertThat(updatedFilm.getMpa().getId()).isEqualTo(4L);
		assertThat(updatedFilm.getMpa().getName()).isEqualTo("R");

		assertThat(updatedFilm.getGenres())
				.extracting(Genre::getId)
				.containsExactly(1L, 6L);
	}

	@Test
	void shouldDeleteFilmFromDatabase() {
		Film film = filmStorage.create(makeValidFilm());
		Long filmId = film.getId();

		filmStorage.delete(filmId);

		assertThatThrownBy(() -> filmStorage.findById(filmId))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Фильм с id " + filmId + " не найден");
	}

	@Test
	void shouldFindAllGenres() {
		Collection<Genre> genres = genreStorage.findAll();

		assertThat(genres).hasSize(6);

		assertThat(genres)
				.extracting(Genre::getId)
				.containsExactly(1L, 2L, 3L, 4L, 5L, 6L);

		assertThat(genres)
				.extracting(Genre::getName)
				.containsExactly(
						"Комедия",
						"Драма",
						"Мультфильм",
						"Триллер",
						"Документальный",
						"Боевик"
				);
	}

	@Test
	void shouldFindGenreById() {
		Genre genre = genreStorage.findById(1L);

		assertThat(genre.getId()).isEqualTo(1L);
		assertThat(genre.getName()).isEqualTo("Комедия");
	}

	@Test
	void shouldThrowExceptionWhenGenreNotFound() {
		assertThatThrownBy(() -> genreStorage.findById(999L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Жанр с id 999 не найден");
	}

	@Test
	void shouldFindAllMpaRatings() {
		Collection<Mpa> ratings = mpaStorage.findAll();

		assertThat(ratings).hasSize(5);

		assertThat(ratings)
				.extracting(Mpa::getId)
				.containsExactly(1L, 2L, 3L, 4L, 5L);

		assertThat(ratings)
				.extracting(Mpa::getName)
				.containsExactly(
						"G",
						"PG",
						"PG-13",
						"R",
						"NC-17"
				);
	}

	@Test
	void shouldFindMpaById() {
		Mpa mpa = mpaStorage.findById(3L);

		assertThat(mpa.getId()).isEqualTo(3L);
		assertThat(mpa.getName()).isEqualTo("PG-13");
	}

	@Test
	void shouldThrowExceptionWhenMpaNotFound() {
		assertThatThrownBy(() -> mpaStorage.findById(999L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Рейтинг MPA с id 999 не найден");
	}

	@Test
	void shouldAddLikeToFilm() {
		User user = userStorage.create(makeValidUser());
		Film film = filmStorage.create(makeValidFilm());

		filmStorage.addLike(film.getId(), user.getId());

		Film storedFilm = filmStorage.findById(film.getId());

		assertThat(storedFilm.getLikes())
				.containsExactly(user.getId());
	}

	@Test
	void shouldNotDuplicateLike() {
		User user = userStorage.create(makeValidUser());
		Film film = filmStorage.create(makeValidFilm());

		filmStorage.addLike(film.getId(), user.getId());
		filmStorage.addLike(film.getId(), user.getId());

		Film storedFilm = filmStorage.findById(film.getId());

		assertThat(storedFilm.getLikes())
				.containsExactly(user.getId());

		assertThat(storedFilm.getLikes()).hasSize(1);
	}

	@Test
	void shouldRemoveLikeFromFilm() {
		User user = userStorage.create(makeValidUser());
		Film film = filmStorage.create(makeValidFilm());

		filmStorage.addLike(film.getId(), user.getId());
		filmStorage.removeLike(film.getId(), user.getId());

		Film storedFilm = filmStorage.findById(film.getId());

		assertThat(storedFilm.getLikes()).isEmpty();
	}

	@Test
	void shouldFindPopularFilmsOrderedByLikesCount() {
		User firstUser = userStorage.create(makeValidUser());

		User secondUser = makeValidUser();
		secondUser.setEmail("petr@mail.ru");
		secondUser.setLogin("petr");
		secondUser.setName("Petr");
		secondUser = userStorage.create(secondUser);

		Film firstFilm = makeValidFilm();
		firstFilm.setName("First film");
		firstFilm = filmStorage.create(firstFilm);

		Film secondFilm = makeValidFilm();
		secondFilm.setName("Second film");
		secondFilm = filmStorage.create(secondFilm);

		Film thirdFilm = makeValidFilm();
		thirdFilm.setName("Third film");
		thirdFilm = filmStorage.create(thirdFilm);

		filmStorage.addLike(firstFilm.getId(), firstUser.getId());
		filmStorage.addLike(firstFilm.getId(), secondUser.getId());

		filmStorage.addLike(secondFilm.getId(), firstUser.getId());

		Collection<Film> popularFilms = filmStorage.findPopular(2, null, null);

		assertThat(popularFilms)
				.extracting(Film::getId)
				.containsExactly(
						firstFilm.getId(),
						secondFilm.getId()
				);
	}

	@Test
	void shouldAddFriendOnlyToRequestSender() {
		User firstUser = userStorage.create(makeValidUser());

		User secondUser = makeValidUser();
		secondUser.setEmail("petr@mail.ru");
		secondUser.setLogin("petr");
		secondUser.setName("Petr");
		secondUser = userStorage.create(secondUser);

		userStorage.addFriend(
				firstUser.getId(),
				secondUser.getId()
		);

		User storedFirstUser =
				userStorage.findById(firstUser.getId());

		User storedSecondUser =
				userStorage.findById(secondUser.getId());

		assertThat(storedFirstUser.getFriends())
				.containsEntry(
						secondUser.getId(),
						FriendshipStatus.UNCONFIRMED
				);

		assertThat(storedSecondUser.getFriends()).isEmpty();
	}

	@Test
	void shouldConfirmFriendshipAfterReverseRequest() {
		User firstUser = userStorage.create(makeValidUser());

		User secondUser = makeValidUser();
		secondUser.setEmail("petr@mail.ru");
		secondUser.setLogin("petr");
		secondUser.setName("Petr");
		secondUser = userStorage.create(secondUser);

		userStorage.addFriend(
				firstUser.getId(),
				secondUser.getId()
		);

		userStorage.addFriend(
				secondUser.getId(),
				firstUser.getId()
		);

		User storedFirstUser =
				userStorage.findById(firstUser.getId());

		User storedSecondUser =
				userStorage.findById(secondUser.getId());

		assertThat(storedFirstUser.getFriends())
				.containsEntry(
						secondUser.getId(),
						FriendshipStatus.CONFIRMED
				);

		assertThat(storedSecondUser.getFriends())
				.containsEntry(
						firstUser.getId(),
						FriendshipStatus.CONFIRMED
				);
	}

	@Test
	void shouldGetUserFriends() {
		User user = userStorage.create(makeValidUser());

		User firstFriend = makeValidUser();
		firstFriend.setEmail("first@mail.ru");
		firstFriend.setLogin("first");
		firstFriend.setName("First");
		firstFriend = userStorage.create(firstFriend);

		User secondFriend = makeValidUser();
		secondFriend.setEmail("second@mail.ru");
		secondFriend.setLogin("second");
		secondFriend.setName("Second");
		secondFriend = userStorage.create(secondFriend);

		userStorage.addFriend(user.getId(), firstFriend.getId());
		userStorage.addFriend(user.getId(), secondFriend.getId());

		Collection<User> friends =
				userStorage.getFriends(user.getId());

		assertThat(friends)
				.extracting(User::getId)
				.containsExactly(
						firstFriend.getId(),
						secondFriend.getId()
				);
	}

	@Test
	void shouldGetCommonFriends() {
		User firstUser = userStorage.create(makeValidUser());

		User secondUser = makeValidUser();
		secondUser.setEmail("second@mail.ru");
		secondUser.setLogin("second");
		secondUser.setName("Second");
		secondUser = userStorage.create(secondUser);

		User commonFriend = makeValidUser();
		commonFriend.setEmail("common@mail.ru");
		commonFriend.setLogin("common");
		commonFriend.setName("Common");
		commonFriend = userStorage.create(commonFriend);

		User onlyFirstFriend = makeValidUser();
		onlyFirstFriend.setEmail("only-first@mail.ru");
		onlyFirstFriend.setLogin("only-first");
		onlyFirstFriend.setName("Only first");
		onlyFirstFriend = userStorage.create(onlyFirstFriend);

		userStorage.addFriend(
				firstUser.getId(),
				commonFriend.getId()
		);

		userStorage.addFriend(
				secondUser.getId(),
				commonFriend.getId()
		);

		userStorage.addFriend(
				firstUser.getId(),
				onlyFirstFriend.getId()
		);

		Collection<User> commonFriends =
				userStorage.getCommonFriends(
						firstUser.getId(),
						secondUser.getId()
				);

		assertThat(commonFriends)
				.extracting(User::getId)
				.containsExactly(commonFriend.getId());
	}

	@Test
	void shouldRemoveFriendship() {
		User firstUser = userStorage.create(makeValidUser());

		User secondUser = makeValidUser();
		secondUser.setEmail("petr@mail.ru");
		secondUser.setLogin("petr");
		secondUser.setName("Petr");
		secondUser = userStorage.create(secondUser);

		userStorage.addFriend(
				firstUser.getId(),
				secondUser.getId()
		);

		userStorage.addFriend(
				secondUser.getId(),
				firstUser.getId()
		);

		userStorage.removeFriend(
				firstUser.getId(),
				secondUser.getId()
		);

		User storedFirstUser =
				userStorage.findById(firstUser.getId());

		User storedSecondUser =
				userStorage.findById(secondUser.getId());

		assertThat(storedFirstUser.getFriends())
				.doesNotContainKey(secondUser.getId());

		assertThat(storedSecondUser.getFriends())
				.containsEntry(
						firstUser.getId(),
						FriendshipStatus.UNCONFIRMED
				);
	}

	@Test
	void shouldReturnCorrectRecommendationsForTargetUser() {
		User userTarget = makeValidUser();
		userTarget.setEmail("target@test.com");
		userTarget.setLogin("target");
		userTarget = userStorage.create(userTarget);

		User userSimilar = makeValidUser();
		userSimilar.setEmail("similar@test.com");
		userSimilar.setLogin("similar");
		userSimilar = userStorage.create(userSimilar);

		User userOther = makeValidUser();
		userOther.setEmail("other@test.com");
		userOther.setLogin("other");
		userOther = userStorage.create(userOther);

		Film filmA = makeValidFilm();
		filmA.setName("Фильм А");
		filmA = filmStorage.create(filmA);

		Film filmB = makeValidFilm();
		filmB.setName("Фильм Б");
		filmB = filmStorage.create(filmB);

		Film filmC = makeValidFilm();
		filmC.setName("Фильм В (Рекомендация)");
		filmC.setGenres(makeGenres(1L, 3L));
		filmC = filmStorage.create(filmC);

		Film filmD = makeValidFilm();
		filmD.setName("Фильм Г");
		filmD = filmStorage.create(filmD);

		filmStorage.addLike(filmA.getId(), userTarget.getId());
		filmStorage.addLike(filmB.getId(), userTarget.getId());

		filmStorage.addLike(filmA.getId(), userSimilar.getId());
		filmStorage.addLike(filmB.getId(), userSimilar.getId());
		filmStorage.addLike(filmC.getId(), userSimilar.getId());

		filmStorage.addLike(filmA.getId(), userOther.getId());
		filmStorage.addLike(filmD.getId(), userOther.getId());

		Collection<Film> recommendations = filmStorage.getRecommendations(userTarget.getId());

		assertThat(recommendations)
				.isNotNull()
				.hasSize(1);

		Film recommendedFilm = recommendations.iterator().next();
		assertThat(recommendedFilm.getName()).isEqualTo("Фильм В (Рекомендация)");

		assertThat(recommendedFilm.getGenres())
				.hasSize(2)
				.extracting(Genre::getId)
				.containsExactlyInAnyOrder(1L, 3L);
	}

	@Test
	void shouldReturnEmptyRecommendationsIfNoCommonLikes() {
		User target = makeValidUser();
		target.setEmail("target2@test.com");
		target = userStorage.create(target);

		User other = makeValidUser();
		other.setEmail("other2@test.com");
		other = userStorage.create(other);

		Film film = makeValidFilm();
		film = filmStorage.create(film);

		filmStorage.addLike(film.getId(), other.getId());

		Collection<Film> recommendations = filmStorage.getRecommendations(target.getId());

		assertThat(recommendations)
				.isNotNull()
				.isEmpty();
	}

	private User makeValidUser() {
		User user = new User();

		user.setEmail("ivan@mail.ru");
		user.setLogin("ivan");
		user.setName("Ivan");
		user.setBirthday(LocalDate.of(2000, 1, 1));

		return user;
	}

	private Film makeValidFilm() {
		Film film = new Film();

		film.setName("Avatar");
		film.setDescription("Good film");
		film.setReleaseDate(LocalDate.of(2009, 12, 10));
		film.setDuration(162);
		film.setMpa(makeMpa(3L));
		film.setGenres(makeGenres(2L, 4L));

		return film;
	}

	private Mpa makeMpa(Long id) {
		Mpa mpa = new Mpa();
		mpa.setId(id);

		return mpa;
	}

	private Set<Genre> makeGenres(Long... ids) {
		Set<Genre> genres = new LinkedHashSet<>();

		for (Long id : ids) {
			Genre genre = new Genre();
			genre.setId(id);
			genres.add(genre);
		}

		return genres;
	}
}