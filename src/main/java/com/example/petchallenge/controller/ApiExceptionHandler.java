package com.example.petchallenge.controller;

import com.example.petchallenge.client.PetStoreUnavailableException;
import com.example.petchallenge.model.ErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
		String details = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.collect(Collectors.joining(", "));

		log.warn("Validación fallida: {}", details);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of("Datos de entrada inválidos", details));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String details = ex.getName() + " debe ser de tipo " + ex.getRequiredType().getSimpleName();
		log.warn("Parámetro inválido: {}", details);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of("Parámetro inválido", details));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
		log.warn("Body de la petición inválido o ausente: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of("Body de la petición inválido o ausente"));
	}

	@ExceptionHandler(PetStoreUnavailableException.class)
	public ResponseEntity<ErrorResponse> handlePetStoreUnavailable(PetStoreUnavailableException ex) {
		log.error("Petstore no disponible: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ErrorResponse.of("Petstore no disponible temporalmente, intenta de nuevo más tarde", ex.getMessage()));
	}

	@ExceptionHandler(RestClientResponseException.class)
	public ResponseEntity<ErrorResponse> handlePetStoreError(RestClientResponseException ex) {
		HttpStatusCode status = ex.getStatusCode();
		log.warn("Error al consumir Petstore -> status: {}, mensaje: {}", status.value(), ex.getMessage());
		return ResponseEntity.status(status)
				.body(ErrorResponse.of("Error al consumir la API de Petstore", ex.getMessage(), status.value()));
	}

	@ExceptionHandler(RequestNotPermitted.class)
	public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RequestNotPermitted ex) {
		log.warn("Límite de peticiones excedido: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(ErrorResponse.of("Límite de peticiones excedido, intenta de nuevo más tarde"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception ex) {
		log.error("Error inesperado", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of("Error inesperado", ex.getMessage()));
	}
}
