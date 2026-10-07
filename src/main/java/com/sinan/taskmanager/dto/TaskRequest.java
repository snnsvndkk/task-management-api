package com.sinan.taskmanager.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.sinan.taskmanager.entity.Task;
import com.sinan.taskmanager.entity.TaskPriority;
import com.sinan.taskmanager.entity.TaskStatus;

public record TaskRequest(
		@NotBlank(message = "Title is required")
		@Size(max = 150, message = "Title must be at most 150 characters")
		String title,

		@Size(max = 2000, message = "Description must be at most 2000 characters")
		String description,

		TaskStatus status,

		TaskPriority priority,

		LocalDate dueDate,

		@NotNull(message = "User id is required")
		@Positive(message = "User id must be a positive number")
		Long userId) {

	/** Copies the task's own fields; the owner is resolved from userId by the service. */
	public Task toEntity() {
		Task task = new Task();
		task.setTitle(title);
		task.setDescription(description);
		task.setStatus(status);
		task.setPriority(priority);
		task.setDueDate(dueDate);
		return task;
	}

}
