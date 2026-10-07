package com.sinan.taskmanager.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Body returned for every failed request. {@code fieldErrors} maps a request
 * field to its validation message and is empty for non-validation errors.
 */
public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		Map<String, String> fieldErrors) {
}
