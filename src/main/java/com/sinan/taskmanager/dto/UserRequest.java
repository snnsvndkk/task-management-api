package com.sinan.taskmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.sinan.taskmanager.entity.User;

public record UserRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 100, message = "Name must be at most 100 characters")
		String name,

		@NotBlank(message = "Email is required")
		@Email(message = "Email must be a valid email address")
		@Size(max = 255, message = "Email must be at most 255 characters")
		String email) {

	public User toEntity() {
		return new User(name, email);
	}

}
