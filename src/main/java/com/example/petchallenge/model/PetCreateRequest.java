package com.example.petchallenge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PetCreateRequest(

		@NotNull
		Long id,

		@NotNull
		PetStatus status,

		@NotBlank
		String name
) {
}
