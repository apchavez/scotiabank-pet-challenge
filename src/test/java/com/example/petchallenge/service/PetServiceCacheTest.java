package com.example.petchallenge.service;

import com.example.petchallenge.client.PetStoreClient;
import com.example.petchallenge.model.PetStoreDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = "spring.cache.type=simple")
class PetServiceCacheTest {

	@MockBean
	private PetStoreClient petStoreClient;

	@Autowired
	private PetService petService;

	@Test
	void getPet_secondCallWithSameId_doesNotHitPetStoreClientAgain() {
		when(petStoreClient.getPet(eq(10L))).thenReturn(new PetStoreDto(10L, "doggie", "available"));

		var first = petService.getPet(10L);
		var second = petService.getPet(10L);

		assertThat(first).isEqualTo(second);
		verify(petStoreClient, times(1)).getPet(10L);
	}

	@Test
	void getPet_differentIds_hitsPetStoreClientForEach() {
		when(petStoreClient.getPet(eq(1L))).thenReturn(new PetStoreDto(1L, "cat", "available"));
		when(petStoreClient.getPet(eq(2L))).thenReturn(new PetStoreDto(2L, "dog", "pending"));

		petService.getPet(1L);
		petService.getPet(2L);

		verify(petStoreClient, times(1)).getPet(1L);
		verify(petStoreClient, times(1)).getPet(2L);
	}
}
