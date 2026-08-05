package com.example.petchallenge.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		String error,
		String details,
		Integer status
) {
	public static ErrorResponse of(String error) {
		return new ErrorResponse(error, null, null);
	}

	public static ErrorResponse of(String error, String details) {
		return new ErrorResponse(error, details, null);
	}

	public static ErrorResponse of(String error, String details, int status) {
		return new ErrorResponse(error, details, status);
	}
}
