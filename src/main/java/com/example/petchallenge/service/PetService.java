package com.example.petchallenge.service;

import com.example.petchallenge.client.PetStoreClient;
import com.example.petchallenge.model.PetCreateRequest;
import com.example.petchallenge.model.PetCreateResponse;
import com.example.petchallenge.model.PetResponse;
import com.example.petchallenge.model.PetStoreDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PetService {

	private static final Logger log = LoggerFactory.getLogger(PetService.class);

	private final PetStoreClient petStoreClient;

	public PetService(PetStoreClient petStoreClient) {
		this.petStoreClient = petStoreClient;
	}

	@Cacheable(value = "pet", key = "#petId")
	public PetResponse getPet(Long petId) {
		log.debug("Buscando pet id: {} (cache miss o cache deshabilitado, se consulta Petstore)", petId);
		PetStoreDto pet = petStoreClient.getPet(petId);

		log.info("Pet obtenido de Petstore -> id: {}, name: {}, status: {}",
				pet.id(), pet.name(), pet.status());

		return new PetResponse(pet.id(), pet.name(), pet.status());
	}

	public PetCreateResponse createPet(PetCreateRequest request) {
		log.debug("Creando pet en Petstore -> id: {}, name: {}, status: {}",
				request.id(), request.name(), request.status());
		PetStoreDto petToCreate = new PetStoreDto(request.id(), request.name(), request.status().name());
		PetStoreDto createdPet = petStoreClient.createPet(petToCreate);

		log.info("Pet creado en Petstore -> id: {}, name: {}, status: {}",
				createdPet.id(), createdPet.name(), createdPet.status());

		String transactionId = UUID.randomUUID().toString();
		LocalDateTime dateCreated = LocalDateTime.now();

		log.debug("Transacción generada -> transactionId: {}, dateCreated: {}", transactionId, dateCreated);

		return new PetCreateResponse(transactionId, dateCreated, createdPet.status(), createdPet.name());
	}
}
