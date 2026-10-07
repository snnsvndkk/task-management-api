package com.sinan.taskmanager.dto;

import java.time.LocalDate;

import com.sinan.taskmanager.entity.Task;
import com.sinan.taskmanager.entity.TaskPriority;
import com.sinan.taskmanager.entity.TaskStatus;

public record TaskRequest(
		String title,
		String description,
		TaskStatus status,
		TaskPriority priority,
		LocalDate dueDate,
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
