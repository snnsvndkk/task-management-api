package com.sinan.taskmanager.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sinan.taskmanager.dto.TaskRequest;
import com.sinan.taskmanager.dto.TaskResponse;
import com.sinan.taskmanager.entity.Task;
import com.sinan.taskmanager.entity.TaskPriority;
import com.sinan.taskmanager.entity.TaskStatus;
import com.sinan.taskmanager.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@PostMapping
	public ResponseEntity<TaskResponse> createTask(@RequestBody TaskRequest request) {
		Task task = taskService.createTask(request.userId(), request.toEntity());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(task.getId())
				.toUri();
		return ResponseEntity.created(location).body(TaskResponse.from(task));
	}

	/** Lists tasks; every filter is optional and they combine with AND. */
	@GetMapping
	public List<TaskResponse> getTasks(
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) TaskPriority priority,
			@RequestParam(required = false) Long userId) {
		return taskService.getTasks(status, priority, userId).stream().map(TaskResponse::from).toList();
	}

	@GetMapping("/{id}")
	public TaskResponse getTask(@PathVariable Long id) {
		return TaskResponse.from(taskService.getTask(id));
	}

	@PutMapping("/{id}")
	public TaskResponse updateTask(@PathVariable Long id, @RequestBody TaskRequest request) {
		return TaskResponse.from(taskService.updateTask(id, request.userId(), request.toEntity()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTask(@PathVariable Long id) {
		taskService.deleteTask(id);
	}

}
