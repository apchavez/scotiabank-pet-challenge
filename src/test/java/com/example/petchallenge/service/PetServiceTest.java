package com.example.petchallenge.service;

import com.example.petchallenge.client.PetStoreClient;
import com.example.petchallenge.model.PetCreateRequest;
import com.example.petchallenge.model.PetCreateResponse;
import com.example.petchallenge.model.PetResponse;
import com.example.petchallenge.model.PetStatus;
import com.example.petchallenge.model.PetStoreDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

	@Mock
	private PetStoreClient petStoreClient;

	@Test
	void getPet_mapsPetStoreDtoToPetResponse() {
		PetService petService = new PetService(petStoreClient);
		when(petStoreClient.getPet(eq(10L))).thenReturn(new PetStoreDto(10L, "doggie", "available"));

		PetResponse response = petService.getPet(10L);

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.name()).isEqualTo("doggie");
		assertThat(response.status()).isEqualTo("available");
	}

	@Test
	void createPet_generatesTransactionIdAndDateCreated_andForwardsToPetStore() {
		PetService petService = new PetService(petStoreClient);
		when(petStoreClient.createPet(any())).thenReturn(new PetStoreDto(100000023L, "testingPet1", "available"));

		LocalDateTime before = LocalDateTime.now();
		PetCreateResponse response = petService.createPet(new PetCreateRequest(100000023L, PetStatus.available, "testingPet1"));
		LocalDateTime after = LocalDateTime.now();

		assertThat(UUID.fromString(response.transactionId())).isNotNull();
		assertThat(response.dateCreated()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
		assertThat(response.name()).isEqualTo("testingPet1");
		assertThat(response.status()).isEqualTo("available");

		ArgumentCaptor<PetStoreDto> captor = ArgumentCaptor.forClass(PetStoreDto.class);
		org.mockito.Mockito.verify(petStoreClient).createPet(captor.capture());
		assertThat(captor.getValue().id()).isEqualTo(100000023L);
		assertThat(captor.getValue().status()).isEqualTo("available");
		assertThat(captor.getValue().name()).isEqualTo("testingPet1");
	}
}
