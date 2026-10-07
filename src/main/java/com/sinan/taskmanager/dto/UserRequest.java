package com.sinan.taskmanager.dto;

import com.sinan.taskmanager.entity.User;

public record UserRequest(String name, String email) {

	public User toEntity() {
		return new User(name, email);
	}

}
