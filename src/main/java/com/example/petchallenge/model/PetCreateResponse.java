package com.example.petchallenge.model;

import java.time.LocalDateTime;

public record PetCreateResponse(
		String transactionId,
		LocalDateTime dateCreated,
		String status,
		String name
) {
}
