package com.sinan.taskmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.sinan.taskmanager.entity.User;
import com.sinan.taskmanager.exception.DuplicateResourceException;
import com.sinan.taskmanager.exception.ResourceNotFoundException;
import com.sinan.taskmanager.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	void createUserSavesUserWithNormalizedNameAndEmail() {
		when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User created = userService.createUser(new User("  Ada Lovelace ", " Ada@Example.COM "));

		assertThat(created.getName()).isEqualTo("Ada Lovelace");
		assertThat(created.getEmail()).isEqualTo("ada@example.com");
		verify(userRepository).save(created);
	}

	@Test
	void createUserRejectsEmailThatIsAlreadyInUse() {
		when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.createUser(new User("Ada", "ada@example.com")))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining("ada@example.com");

		verify(userRepository, never()).save(any());
	}

	@Test
	void getUserReturnsExistingUser() {
		User user = existingUser(1L, "Ada", "ada@example.com");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		assertThat(userService.getUser(1L)).isSameAs(user);
	}

	@Test
	void getUserThrowsWhenUserDoesNotExist() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUser(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User with id 99 not found");
	}

	@Test
	void getAllUsersReturnsUsersOrderedById() {
		List<User> users = List.of(
				existingUser(1L, "Ada", "ada@example.com"),
				existingUser(2L, "Alan", "alan@example.com"));
		when(userRepository.findAll(Sort.by("id"))).thenReturn(users);

		assertThat(userService.getAllUsers()).isEqualTo(users);
	}

	@Test
	void updateUserAppliesNewNameAndEmail() {
		User user = existingUser(1L, "Ada", "ada@example.com");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmailAndIdNot("countess@example.com", 1L)).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User updated = userService.updateUser(1L, new User("Ada King", "Countess@Example.com"));

		assertThat(updated).isSameAs(user);
		assertThat(updated.getName()).isEqualTo("Ada King");
		assertThat(updated.getEmail()).isEqualTo("countess@example.com");
	}

	@Test
	void updateUserRejectsEmailOwnedByAnotherUser() {
		User user = existingUser(1L, "Ada", "ada@example.com");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmailAndIdNot("alan@example.com", 1L)).thenReturn(true);

		assertThatThrownBy(() -> userService.updateUser(1L, new User("Ada", "alan@example.com")))
				.isInstanceOf(DuplicateResourceException.class);

		assertThat(user.getEmail()).isEqualTo("ada@example.com");
		verify(userRepository, never()).save(any());
	}

	@Test
	void updateUserThrowsWhenUserDoesNotExist() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.updateUser(99L, new User("Ada", "ada@example.com")))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	void deleteUserRemovesExistingUser() {
		User user = existingUser(1L, "Ada", "ada@example.com");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		userService.deleteUser(1L);

		verify(userRepository).delete(user);
	}

	@Test
	void deleteUserThrowsWhenUserDoesNotExist() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.deleteUser(99L))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).delete(any());
	}

	private static User existingUser(Long id, String name, String email) {
		User user = new User(name, email);
		user.setId(id);
		return user;
	}

}
