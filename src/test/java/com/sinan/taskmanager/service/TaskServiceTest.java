package com.sinan.taskmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.sinan.taskmanager.entity.Task;
import com.sinan.taskmanager.entity.TaskPriority;
import com.sinan.taskmanager.entity.TaskStatus;
import com.sinan.taskmanager.entity.User;
import com.sinan.taskmanager.exception.ResourceNotFoundException;
import com.sinan.taskmanager.repository.TaskRepository;
import com.sinan.taskmanager.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	private static final LocalDate DUE_DATE = LocalDate.of(2026, 12, 31);

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private TaskService taskService;

	@Test
	void createTaskAssignsOwnerAndDefaultsStatusAndPriority() {
		User owner = user(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task created = taskService.createTask(1L, task("  Write report ", null, null));

		assertThat(created.getUser()).isSameAs(owner);
		assertThat(created.getTitle()).isEqualTo("Write report");
		assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
		assertThat(created.getPriority()).isEqualTo(TaskPriority.MEDIUM);
		verify(taskRepository).save(created);
	}

	@Test
	void createTaskKeepsExplicitStatusAndPriority() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task created = taskService.createTask(1L, task("Write report", TaskStatus.IN_PROGRESS, TaskPriority.HIGH));

		assertThat(created.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
		assertThat(created.getPriority()).isEqualTo(TaskPriority.HIGH);
	}

	@Test
	void createTaskThrowsWhenOwnerDoesNotExist() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.createTask(99L, task("Write report", null, null)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User with id 99 not found");

		verify(taskRepository, never()).save(any());
	}

	@Test
	void getTaskReturnsExistingTask() {
		Task task = existingTask(10L, user(1L));
		when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

		assertThat(taskService.getTask(10L)).isSameAs(task);
	}

	@Test
	void getTaskThrowsWhenTaskDoesNotExist() {
		when(taskRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.getTask(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Task with id 99 not found");
	}

	@Test
	void getTasksReturnsRepositoryResultsOrderedById() {
		List<Task> tasks = List.of(existingTask(10L, user(1L)), existingTask(11L, user(1L)));
		when(taskRepository.findAll(ArgumentMatchers.<Specification<Task>>any(), eq(Sort.by("id"))))
				.thenReturn(tasks);

		List<Task> result = taskService.getTasks(TaskStatus.TODO, TaskPriority.HIGH, 1L);

		assertThat(result).isEqualTo(tasks);
	}

	@Test
	void updateTaskReplacesFields() {
		Task task = existingTask(10L, user(1L));
		when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task changes = task("Submit report", TaskStatus.DONE, TaskPriority.HIGH);
		changes.setDescription("Final version");
		changes.setDueDate(DUE_DATE);
		Task updated = taskService.updateTask(10L, 1L, changes);

		assertThat(updated).isSameAs(task);
		assertThat(updated.getTitle()).isEqualTo("Submit report");
		assertThat(updated.getDescription()).isEqualTo("Final version");
		assertThat(updated.getDueDate()).isEqualTo(DUE_DATE);
		assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
		assertThat(updated.getPriority()).isEqualTo(TaskPriority.HIGH);
		// Owner is unchanged, so there is no reason to look the user up again.
		verify(userRepository, never()).findById(any());
	}

	@Test
	void updateTaskKeepsStatusAndPriorityWhenOmitted() {
		Task task = existingTask(10L, user(1L));
		task.setStatus(TaskStatus.IN_PROGRESS);
		task.setPriority(TaskPriority.LOW);
		when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task updated = taskService.updateTask(10L, 1L, task("Renamed", null, null));

		assertThat(updated.getTitle()).isEqualTo("Renamed");
		assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
		assertThat(updated.getPriority()).isEqualTo(TaskPriority.LOW);
	}

	@Test
	void updateTaskReassignsToAnotherUser() {
		Task task = existingTask(10L, user(1L));
		User newOwner = user(2L);
		when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
		when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task updated = taskService.updateTask(10L, 2L, task("Write report", null, null));

		assertThat(updated.getUser()).isSameAs(newOwner);
	}

	@Test
	void updateTaskThrowsWhenNewOwnerDoesNotExist() {
		Task task = existingTask(10L, user(1L));
		when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.updateTask(10L, 99L, task("Write report", null, null)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User with id 99 not found");

		verify(taskRepository, never()).save(any());
	}

	@Test
	void updateTaskThrowsWhenTaskDoesNotExist() {
		when(taskRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.updateTask(99L, 1L, task("Write report", null, null)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(taskRepository, never()).save(any());
	}

	@Test
	void deleteTaskRemovesExistingTask() {
		Task task = existingTask(10L, user(1L));
		when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

		taskService.deleteTask(10L);

		verify(taskRepository).delete(task);
	}

	@Test
	void deleteTaskThrowsWhenTaskDoesNotExist() {
		when(taskRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.deleteTask(99L))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(taskRepository, never()).delete(any(Task.class));
	}

	private static User user(Long id) {
		User user = new User("User " + id, "user" + id + "@example.com");
		user.setId(id);
		return user;
	}

	private static Task task(String title, TaskStatus status, TaskPriority priority) {
		Task task = new Task();
		task.setTitle(title);
		task.setStatus(status);
		task.setPriority(priority);
		return task;
	}

	private static Task existingTask(Long id, User owner) {
		Task task = task("Write report", TaskStatus.TODO, TaskPriority.MEDIUM);
		task.setId(id);
		task.setUser(owner);
		return task;
	}

}
