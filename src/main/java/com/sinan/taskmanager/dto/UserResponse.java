package com.sinan.taskmanager.dto;

import java.time.Instant;

import com.sinan.taskmanager.entity.User;

public record UserResponse(Long id, String name, String email, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
	}

}
