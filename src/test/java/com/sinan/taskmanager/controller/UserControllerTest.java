package com.sinan.taskmanager.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sinan.taskmanager.entity.User;
import com.sinan.taskmanager.exception.DuplicateResourceException;
import com.sinan.taskmanager.exception.ResourceNotFoundException;
import com.sinan.taskmanager.service.UserService;

@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	void createUserReturnsCreatedUserWithLocation() throws Exception {
		when(userService.createUser(any(User.class))).thenReturn(savedUser());

		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Ada Lovelace", "email": "ada@example.com"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/users/1")))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Ada Lovelace"))
				.andExpect(jsonPath("$.email").value("ada@example.com"))
				.andExpect(jsonPath("$.createdAt").value("2026-01-15T10:00:00Z"))
				.andExpect(jsonPath("$.tasks").doesNotExist());
	}

	@Test
	void createUserWithInvalidBodyReturnsFieldErrors() throws Exception {
		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": " ", "email": "not-an-email"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.path").value("/api/users"))
				.andExpect(jsonPath("$.fieldErrors.name").value("Name is required"))
				.andExpect(jsonPath("$.fieldErrors.email").value("Email must be a valid email address"));

		verifyNoInteractions(userService);
	}

	@Test
	void createUserWithDuplicateEmailReturnsConflict() throws Exception {
		when(userService.createUser(any(User.class)))
				.thenThrow(new DuplicateResourceException("Email ada@example.com is already in use"));

		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Ada Lovelace", "email": "ada@example.com"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Email ada@example.com is already in use"));
	}

	@Test
	void getUserReturnsNotFoundForUnknownId() throws Exception {
		when(userService.getUser(99L)).thenThrow(new ResourceNotFoundException("User", 99L));

		mockMvc.perform(get("/api/users/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("User with id 99 not found"))
				.andExpect(jsonPath("$.path").value("/api/users/99"));
	}

	@Test
	void getUserWithNonNumericIdReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/users/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
	}

	@Test
	void deleteUserReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/users/1"))
				.andExpect(status().isNoContent());

		verify(userService).deleteUser(1L);
	}

	private static User savedUser() {
		User user = new User("Ada Lovelace", "ada@example.com");
		user.setId(1L);
		user.setCreatedAt(Instant.parse("2026-01-15T10:00:00Z"));
		return user;
	}

}
