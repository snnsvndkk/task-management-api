package com.sinan.taskmanager.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sinan.taskmanager.dto.UserRequest;
import com.sinan.taskmanager.dto.UserResponse;
import com.sinan.taskmanager.entity.User;
import com.sinan.taskmanager.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
		User user = userService.createUser(request.toEntity());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(user.getId())
				.toUri();
		return ResponseEntity.created(location).body(UserResponse.from(user));
	}

	@GetMapping
	public List<UserResponse> getAllUsers() {
		return userService.getAllUsers().stream().map(UserResponse::from).toList();
	}

	@GetMapping("/{id}")
	public UserResponse getUser(@PathVariable Long id) {
		return UserResponse.from(userService.getUser(id));
	}

	@PutMapping("/{id}")
	public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
		return UserResponse.from(userService.updateUser(id, request.toEntity()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
	}

}
