package com.example.petchallenge.controller;

import com.example.petchallenge.model.PetCreateRequest;
import com.example.petchallenge.model.PetCreateResponse;
import com.example.petchallenge.model.PetResponse;
import com.example.petchallenge.service.PetService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pet")
public class PetController {

	private static final Logger log = LoggerFactory.getLogger(PetController.class);

	private final PetService petService;

	public PetController(PetService petService) {
		this.petService = petService;
	}

	@RateLimiter(name = "petApi")
	@GetMapping("/{idPet}")
	public PetResponse getPet(@PathVariable("idPet") Long idPet) {
		log.info("GET /api/pet/{} recibido", idPet);
		PetResponse response = petService.getPet(idPet);
		log.info("GET /api/pet/{} respondido -> name: {}, status: {}", idPet, response.name(), response.status());
		return response;
	}

	@RateLimiter(name = "petApi")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PetCreateResponse createPet(@Valid @RequestBody PetCreateRequest request) {
		log.info("POST /api/pet recibido -> id: {}, name: {}, status: {}", request.id(), request.name(), request.status());
		PetCreateResponse response = petService.createPet(request);
		log.info("POST /api/pet respondido -> transactionId: {}", response.transactionId());
		return response;
	}
}
