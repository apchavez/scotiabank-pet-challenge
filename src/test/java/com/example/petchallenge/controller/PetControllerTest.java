package com.example.petchallenge.controller;

import com.example.petchallenge.model.PetCreateResponse;
import com.example.petchallenge.model.PetResponse;
import com.example.petchallenge.security.ApiKeyAuthFilter;
import com.example.petchallenge.security.SecurityConfig;
import com.example.petchallenge.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
@Import({SecurityConfig.class, ApiKeyAuthFilter.class})
class PetControllerTest {

	private static final String VALID_API_KEY = "change-me-in-prod";
	private static final String API_KEY_HEADER = "X-Api-Key";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PetService petService;

	@Test
	void getPet_returnsIdNameStatus() throws Exception {
		when(petService.getPet(eq(10L))).thenReturn(new PetResponse(10L, "doggie", "available"));

		mockMvc.perform(get("/api/pet/10").header(API_KEY_HEADER, VALID_API_KEY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.name").value("doggie"))
				.andExpect(jsonPath("$.status").value("available"));
	}

	@Test
	void getPet_petStoreNotFound_returnsControlledError() throws Exception {
		when(petService.getPet(eq(999L))).thenThrow(
				HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

		mockMvc.perform(get("/api/pet/999").header(API_KEY_HEADER, VALID_API_KEY))
				.andExpect(status().isNotFound());
	}

	@Test
	void getPet_nonNumericIdPet_returnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/pet/abc").header(API_KEY_HEADER, VALID_API_KEY))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createPet_malformedJsonBody_returnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/pet")
						.header(API_KEY_HEADER, VALID_API_KEY)
						.contentType("application/json")
						.content("{\"id\":1,"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createPet_missingBody_returnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/pet").header(API_KEY_HEADER, VALID_API_KEY).contentType("application/json"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createPet_invalidStatusValue_returnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/pet")
						.header(API_KEY_HEADER, VALID_API_KEY)
						.contentType("application/json")
						.content("{\"id\":1,\"status\":\"not-a-valid-status\",\"name\":\"x\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createPet_returnsTransactionIdAndDateCreated() throws Exception {
		when(petService.createPet(any())).thenReturn(
				new PetCreateResponse("b3f1c2e0-0000-0000-0000-000000000000", LocalDateTime.now(), "available", "testingPet1"));

		mockMvc.perform(post("/api/pet")
						.header(API_KEY_HEADER, VALID_API_KEY)
						.contentType("application/json")
						.content("{\"id\":100000023,\"status\":\"available\",\"name\":\"testingPet1\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.transactionId").isNotEmpty())
				.andExpect(jsonPath("$.dateCreated").isNotEmpty())
				.andExpect(jsonPath("$.name").value("testingPet1"))
				.andExpect(jsonPath("$.status").value("available"));
	}

	@Test
	void getPet_missingApiKey_returnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/pet/10"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").isNotEmpty());
	}

	@Test
	void getPet_wrongApiKey_returnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/pet/10").header(API_KEY_HEADER, "wrong-key"))
				.andExpect(status().isUnauthorized());
	}
}
