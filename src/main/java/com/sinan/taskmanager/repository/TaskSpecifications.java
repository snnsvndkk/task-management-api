package com.sinan.taskmanager.repository;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.sinan.taskmanager.entity.Task;
import com.sinan.taskmanager.entity.TaskPriority;
import com.sinan.taskmanager.entity.TaskStatus;

public final class TaskSpecifications {

	private TaskSpecifications() {
	}

	/**
	 * Builds a filter from whichever criteria are present; a null criterion is
	 * ignored, so passing all nulls matches every task.
	 */
	public static Specification<Task> matching(TaskStatus status, TaskPriority priority, Long userId) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (priority != null) {
				predicates.add(cb.equal(root.get("priority"), priority));
			}
			if (userId != null) {
				predicates.add(cb.equal(root.get("user").get("id"), userId));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

}
