package com.sinan.taskmanager.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sinan.taskmanager.entity.Task;
import com.sinan.taskmanager.entity.TaskPriority;
import com.sinan.taskmanager.entity.TaskStatus;
import com.sinan.taskmanager.entity.User;
import com.sinan.taskmanager.service.TaskService;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskService taskService;

	@Test
	void createTaskReturnsCreatedTaskWithLocation() throws Exception {
		when(taskService.createTask(eq(1L), any(Task.class))).thenReturn(savedTask());

		mockMvc.perform(post("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "Write report", "priority": "HIGH", "dueDate": "2026-12-31", "userId": 1}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/tasks/10")))
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.title").value("Write report"))
				.andExpect(jsonPath("$.status").value("TODO"))
				.andExpect(jsonPath("$.priority").value("HIGH"))
				.andExpect(jsonPath("$.dueDate").value("2026-12-31"))
				.andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.user").doesNotExist());
	}

	@Test
	void createTaskWithInvalidBodyReturnsFieldErrors() throws Exception {
		mockMvc.perform(post("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": ""}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.title").value("Title is required"))
				.andExpect(jsonPath("$.fieldErrors.userId").value("User id is required"));

		verifyNoInteractions(taskService);
	}

	@Test
	void createTaskWithUnknownStatusReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "Write report", "status": "BLOCKED", "userId": 1}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Invalid value 'BLOCKED' for field 'status'. Allowed values: TODO, IN_PROGRESS, DONE"));
	}

	@Test
	void createTaskWithInvalidDueDateNamesTheField() throws Exception {
		mockMvc.perform(post("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "Write report", "dueDate": "2026-13-45", "userId": 1}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value(
						"Invalid value '2026-13-45' for field 'dueDate'. Expected a date in the format YYYY-MM-DD"));

		verifyNoInteractions(taskService);
	}

	@Test
	void createTaskWithNonNumericUserIdNamesTheField() throws Exception {
		mockMvc.perform(post("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "Write report", "userId": "abc"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid value 'abc' for field 'userId'"));
	}

	@Test
	void createTaskWithMalformedJsonReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed JSON request body"));
	}

	@Test
	void getTasksPassesFiltersToService() throws Exception {
		when(taskService.getTasks(TaskStatus.TODO, TaskPriority.HIGH, 1L)).thenReturn(List.of(savedTask()));

		mockMvc.perform(get("/api/tasks")
				.param("status", "TODO")
				.param("priority", "HIGH")
				.param("userId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(10));
	}

	@Test
	void getTasksWithoutFiltersListsEverything() throws Exception {
		when(taskService.getTasks(null, null, null)).thenReturn(List.of());

		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		verify(taskService).getTasks(null, null, null);
	}

	@Test
	void getTasksWithUnknownPriorityReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/tasks").param("priority", "URGENT"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value(
						"Invalid value 'URGENT' for parameter 'priority'. Allowed values: LOW, MEDIUM, HIGH"));
	}

	@Test
	void unsupportedMethodReturnsMethodNotAllowedInTheSameErrorShape() throws Exception {
		mockMvc.perform(patch("/api/tasks/10"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.error").value("Method Not Allowed"))
				.andExpect(jsonPath("$.path").value("/api/tasks/10"));
	}

	@Test
	void unknownPathReturnsNotFoundInTheSameErrorShape() throws Exception {
		mockMvc.perform(get("/api/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("No endpoint found for GET /api/unknown"))
				.andExpect(jsonPath("$.path").value("/api/unknown"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	private static Task savedTask() {
		User owner = new User("Ada Lovelace", "ada@example.com");
		owner.setId(1L);
		Task task = new Task();
		task.setId(10L);
		task.setTitle("Write report");
		task.setStatus(TaskStatus.TODO);
		task.setPriority(TaskPriority.HIGH);
		task.setDueDate(LocalDate.of(2026, 12, 31));
		task.setCreatedAt(Instant.parse("2026-01-15T10:00:00Z"));
		task.setUpdatedAt(Instant.parse("2026-01-15T10:00:00Z"));
		task.setUser(owner);
		return task;
	}

}
