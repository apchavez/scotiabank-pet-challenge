package com.example.petchallenge.client;

import com.example.petchallenge.model.PetStoreDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class PetStoreClient {

	private static final Logger log = LoggerFactory.getLogger(PetStoreClient.class);

	private final RestClient restClient;

	public PetStoreClient(RestClient.Builder restClientBuilder, @Value("${petstore.base-url}") String baseUrl) {
		this.restClient = restClientBuilder
				.baseUrl(baseUrl)
				.build();
	}

	@Retry(name = "petstore")
	@CircuitBreaker(name = "petstore", fallbackMethod = "getPetFallback")
	public PetStoreDto getPet(Long petId) {
		log.debug("Llamando a Petstore -> GET /pet/{}", petId);
		long start = System.currentTimeMillis();
		PetStoreDto pet = restClient.get()
				.uri("/pet/{petId}", petId)
				.retrieve()
				.body(PetStoreDto.class);
		log.info("Petstore respondió GET /pet/{} en {} ms", petId, System.currentTimeMillis() - start);
		return pet;
	}

	@SuppressWarnings("unused")
	private PetStoreDto getPetFallback(Long petId, Throwable ex) {
		log.warn("Fallback activado para GET /pet/{} -> {}: {}", petId, ex.getClass().getSimpleName(), ex.getMessage());
		return handleFallback(ex, "No fue posible obtener el pet " + petId + " de Petstore");
	}

	@Retry(name = "petstore")
	@CircuitBreaker(name = "petstore", fallbackMethod = "createPetFallback")
	public PetStoreDto createPet(PetStoreDto pet) {
		log.debug("Llamando a Petstore -> POST /pet (id: {})", pet.id());
		long start = System.currentTimeMillis();
		PetStoreDto created = restClient.post()
				.uri("/pet")
				.body(pet)
				.retrieve()
				.body(PetStoreDto.class);
		log.info("Petstore respondió POST /pet en {} ms", System.currentTimeMillis() - start);
		return created;
	}

	@SuppressWarnings("unused")
	private PetStoreDto createPetFallback(PetStoreDto pet, Throwable ex) {
		log.warn("Fallback activado para POST /pet (id: {}) -> {}: {}", pet.id(), ex.getClass().getSimpleName(), ex.getMessage());
		return handleFallback(ex, "No fue posible crear el pet en Petstore");
	}

	private PetStoreDto handleFallback(Throwable ex, String unavailableMessage) {
		if (ex instanceof HttpClientErrorException httpClientErrorException) {
			log.warn("Petstore respondió error de cliente {} -> {}", httpClientErrorException.getStatusCode(), unavailableMessage);
			throw httpClientErrorException;
		}
		log.error("Petstore no disponible -> {}", unavailableMessage, ex);
		throw new PetStoreUnavailableException(unavailableMessage, ex);
	}
}
