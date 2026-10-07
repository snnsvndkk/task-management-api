package com.sinan.taskmanager.exception;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sinan.taskmanager.dto.ApiError;

import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Translates every exception into an {@link ApiError} body. Extending
 * {@link ResponseEntityExceptionHandler} keeps Spring MVC's own status codes
 * (405, 415, 404 for unknown paths, ...) while giving them the same JSON shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static final String UNEXPECTED_ERROR = "An unexpected error occurred";

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<Object> handleDuplicate(DuplicateResourceException ex, WebRequest request) {
		return error(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	// Safety net for constraint violations that slip past the service checks,
	// e.g. two concurrent requests registering the same email.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
		return error(HttpStatus.CONFLICT, "Request conflicts with existing data", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
		log.error("Unhandled exception", ex);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new TreeMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return build(status, "Validation failed", fieldErrors, headers, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String message = "Malformed JSON request body";
		// Well-formed JSON whose value cannot be converted to the field's type,
		// e.g. an unknown enum constant or an impossible date.
		if (ex.getCause() instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()
				&& invalid.getPath().getLast().getPropertyName() != null) {
			String field = invalid.getPath().getLast().getPropertyName();
			message = invalidValue(invalid.getValue(), "field", field, invalid.getTargetType());
		}
		return build(status, message, Map.of(), headers, request);
	}

	// Unknown paths fall through to the static resource handler, whose own
	// message talks about static resources rather than API endpoints.
	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return build(status, endpointNotFound(request), Map.of(), headers, request);
	}

	@Override
	protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return build(status, endpointNotFound(request), Map.of(), headers, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
				: ex.getPropertyName();
		String message = invalidValue(ex.getValue(), "parameter", name, ex.getRequiredType());
		return build(status, message, Map.of(), headers, request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		String message;
		if (statusCode.is5xxServerError()) {
			log.error("Request failed with status {}", statusCode.value(), ex);
			message = UNEXPECTED_ERROR;
		}
		else {
			message = ex instanceof ErrorResponse response ? response.getBody().getDetail() : ex.getMessage();
		}
		return build(statusCode, message, Map.of(), headers, request);
	}

	private ResponseEntity<Object> error(HttpStatus status, String message, WebRequest request) {
		return build(status, message, Map.of(), new HttpHeaders(), request);
	}

	private ResponseEntity<Object> build(HttpStatusCode status, String message, Map<String, String> fieldErrors,
			HttpHeaders headers, WebRequest request) {
		HttpStatus resolved = HttpStatus.resolve(status.value());
		String error = resolved != null ? resolved.getReasonPhrase() : "Error";
		String path = request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : "";
		ApiError body = new ApiError(Instant.now(), status.value(), error, message, path, fieldErrors);
		return new ResponseEntity<>(body, headers, status);
	}

	private static String endpointNotFound(WebRequest request) {
		if (request instanceof ServletWebRequest servlet) {
			return "No endpoint found for " + servlet.getRequest().getMethod() + " "
					+ servlet.getRequest().getRequestURI();
		}
		return "No endpoint found";
	}

	private static String invalidValue(Object value, String kind, String name, Class<?> type) {
		String message = "Invalid value '" + value + "' for " + kind + " '" + name + "'";
		if (type != null && type.isEnum()) {
			message += ". Allowed values: " + String.join(", ",
					Arrays.stream(type.getEnumConstants()).map(Object::toString).toList());
		}
		else if (type == LocalDate.class) {
			message += ". Expected a date in the format YYYY-MM-DD";
		}
		return message;
	}

}
