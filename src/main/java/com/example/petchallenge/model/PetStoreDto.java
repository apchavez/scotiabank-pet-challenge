package com.example.petchallenge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PetStoreDto(
		Long id,
		String name,
		String status
) {
}
