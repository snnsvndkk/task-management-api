package com.sinan.taskmanager.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AuditTimestampTest {

	@Test
	void userCreatedAtHasMicrosecondPrecision() {
		User user = new User("Ada", "ada@example.com");

		user.onCreate();

		assertMicrosecondPrecision(user.getCreatedAt());
	}

	@Test
	void taskTimestampsHaveMicrosecondPrecision() {
		Task task = new Task();

		task.onCreate();

		assertMicrosecondPrecision(task.getCreatedAt());
		assertThat(task.getUpdatedAt()).isEqualTo(task.getCreatedAt());

		task.onUpdate();

		assertMicrosecondPrecision(task.getUpdatedAt());
		assertThat(task.getUpdatedAt()).isAfterOrEqualTo(task.getCreatedAt());
	}

	// Anything finer than a microsecond is dropped by PostgreSQL on write.
	private static void assertMicrosecondPrecision(Instant instant) {
		assertThat(instant.getNano() % 1_000).isZero();
	}

}
