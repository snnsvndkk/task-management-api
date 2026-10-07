package com.sinan.taskmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Loads the full context against the real PostgreSQL database, so it only runs
// when DB_PASSWORD is set. Every other test runs without a database.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class TaskmanagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
