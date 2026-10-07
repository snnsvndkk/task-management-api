package com.sinan.taskmanager.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.sinan.taskmanager.entity.Task;
import com.sinan.taskmanager.entity.TaskPriority;
import com.sinan.taskmanager.entity.TaskStatus;

public record TaskResponse(
		Long id,
		String title,
		String description,
		TaskStatus status,
		TaskPriority priority,
		LocalDate dueDate,
		Instant createdAt,
		Instant updatedAt,
		Long userId) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getPriority(),
				task.getDueDate(),
				task.getCreatedAt(),
				task.getUpdatedAt(),
				task.getUser().getId());
	}

}
