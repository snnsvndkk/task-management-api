package com.sinan.taskmanager.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sinan.taskmanager.entity.Task;
import com.sinan.taskmanager.entity.TaskPriority;
import com.sinan.taskmanager.entity.TaskStatus;
import com.sinan.taskmanager.entity.User;
import com.sinan.taskmanager.exception.ResourceNotFoundException;
import com.sinan.taskmanager.repository.TaskRepository;
import com.sinan.taskmanager.repository.TaskSpecifications;
import com.sinan.taskmanager.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TaskService {

	private final TaskRepository taskRepository;
	private final UserRepository userRepository;

	public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
		this.taskRepository = taskRepository;
		this.userRepository = userRepository;
	}

	/** Creates a task owned by the given user, defaulting to TODO / MEDIUM. */
	@Transactional
	public Task createTask(Long userId, Task task) {
		task.setUser(findUser(userId));
		task.setTitle(task.getTitle().trim());
		if (task.getStatus() == null) {
			task.setStatus(TaskStatus.TODO);
		}
		if (task.getPriority() == null) {
			task.setPriority(TaskPriority.MEDIUM);
		}
		return taskRepository.save(task);
	}

	public Task getTask(Long id) {
		return taskRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Task", id));
	}

	/** Lists tasks, narrowed by whichever of the filters are non-null. */
	public List<Task> getTasks(TaskStatus status, TaskPriority priority, Long userId) {
		return taskRepository.findAll(TaskSpecifications.matching(status, priority, userId), Sort.by("id"));
	}

	/**
	 * Replaces the task's fields with those in {@code changes}. A null status or
	 * priority keeps the current value, since neither column is nullable.
	 */
	@Transactional
	public Task updateTask(Long id, Long userId, Task changes) {
		Task task = getTask(id);
		if (!task.getUser().getId().equals(userId)) {
			task.setUser(findUser(userId));
		}
		task.setTitle(changes.getTitle().trim());
		task.setDescription(changes.getDescription());
		task.setDueDate(changes.getDueDate());
		if (changes.getStatus() != null) {
			task.setStatus(changes.getStatus());
		}
		if (changes.getPriority() != null) {
			task.setPriority(changes.getPriority());
		}
		return taskRepository.save(task);
	}

	@Transactional
	public void deleteTask(Long id) {
		taskRepository.delete(getTask(id));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
	}

}
