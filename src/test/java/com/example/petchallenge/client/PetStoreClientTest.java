package com.example.petchallenge.client;

import com.example.petchallenge.model.PetStoreDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class PetStoreClientTest {

	private static final String BASE_URL = "https://petstore.swagger.io/v2";

	@Test
	void getPet_callsExternalApiAndMapsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PetStoreClient client = new PetStoreClient(builder, BASE_URL);

		server.expect(requestTo(BASE_URL + "/pet/10"))
				.andExpect(method(GET))
				.andRespond(withSuccess("{\"id\":10,\"name\":\"doggie\",\"status\":\"available\"}", MediaType.APPLICATION_JSON));

		PetStoreDto pet = client.getPet(10L);

		assertThat(pet.id()).isEqualTo(10L);
		assertThat(pet.name()).isEqualTo("doggie");
		assertThat(pet.status()).isEqualTo("available");
		server.verify();
	}

	@Test
	void createPet_postsToExternalApiAndMapsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PetStoreClient client = new PetStoreClient(builder, BASE_URL);

		server.expect(requestTo(BASE_URL + "/pet"))
				.andExpect(method(POST))
				.andRespond(withSuccess("{\"id\":100000023,\"name\":\"testingPet1\",\"status\":\"available\"}", MediaType.APPLICATION_JSON));

		PetStoreDto created = client.createPet(new PetStoreDto(100000023L, "testingPet1", "available"));

		assertThat(created.id()).isEqualTo(100000023L);
		assertThat(created.name()).isEqualTo("testingPet1");
		assertThat(created.status()).isEqualTo("available");
		server.verify();
	}
}
