package com.example.petchallenge;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.cache.type=simple")
class PetChallengeIntegrationTest {

	private static final String VALID_API_KEY = "change-me-in-prod";
	private static final String API_KEY_HEADER = "X-Api-Key";

	private static WireMockServer wireMockServer;

	@LocalServerPort
	private int port;

	private final TestRestTemplate restTemplate = new TestRestTemplate();

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@BeforeAll
	static void startWireMock() {
		wireMockServer = new WireMockServer(0);
		wireMockServer.start();
	}

	@AfterAll
	static void stopWireMock() {
		wireMockServer.stop();
	}

	@BeforeEach
	void resetCircuitBreaker() {
		circuitBreakerRegistry.circuitBreaker("petstore").reset();
	}

	@AfterEach
	void resetStubs() {
		wireMockServer.resetAll();
	}

	@DynamicPropertySource
	static void petstoreBaseUrl(DynamicPropertyRegistry registry) {
		registry.add("petstore.base-url", () -> "http://localhost:" + wireMockServer.port() + "/v2");
	}

	private HttpEntity<Void> authEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(API_KEY_HEADER, VALID_API_KEY);
		return new HttpEntity<>(headers);
	}

	@Test
	void getPet_endToEnd_returnsMappedResponse() {
		wireMockServer.stubFor(get(urlEqualTo("/v2/pet/10"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"id\":10,\"name\":\"doggie\",\"status\":\"available\"}")));

		ResponseEntity<Map> response = restTemplate.exchange(
				"http://localhost:" + port + "/api/pet/10", HttpMethod.GET, authEntity(), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("id", 10).containsEntry("name", "doggie").containsEntry("status", "available");
	}

	@Test
	void getPet_petStoreNotFound_endToEnd_returns404() {
		wireMockServer.stubFor(get(urlEqualTo("/v2/pet/999"))
				.willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json")
						.withBody("{\"message\":\"Pet not found\"}")));

		ResponseEntity<Map> response = restTemplate.exchange(
				"http://localhost:" + port + "/api/pet/999", HttpMethod.GET, authEntity(), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		wireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/pet/999")));
	}

	@Test
	void getPet_transientNetworkFailure_retriesAndSucceeds() {
		wireMockServer.stubFor(get(urlEqualTo("/v2/pet/50"))
				.inScenario("retry-scenario")
				.whenScenarioStateIs(STARTED)
				.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
				.willSetStateTo("second-attempt"));

		wireMockServer.stubFor(get(urlEqualTo("/v2/pet/50"))
				.inScenario("retry-scenario")
				.whenScenarioStateIs("second-attempt")
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"id\":50,\"name\":\"retry-cat\",\"status\":\"available\"}")));

		ResponseEntity<Map> response = restTemplate.exchange(
				"http://localhost:" + port + "/api/pet/50", HttpMethod.GET, authEntity(), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("name", "retry-cat");
	}

	@Test
	void getPet_allRetriesFail_returns503() {
		wireMockServer.stubFor(get(urlEqualTo("/v2/pet/60"))
				.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		ResponseEntity<Map> response = restTemplate.exchange(
				"http://localhost:" + port + "/api/pet/60", HttpMethod.GET, authEntity(), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void createPet_endToEnd_generatesTransactionIdAndForwardsToPetStore() {
		wireMockServer.stubFor(post(urlEqualTo("/v2/pet"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"id\":100000023,\"name\":\"testingPet1\",\"status\":\"available\"}")));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(API_KEY_HEADER, VALID_API_KEY);
		HttpEntity<String> request = new HttpEntity<>(
				"{\"id\":100000023,\"status\":\"available\",\"name\":\"testingPet1\"}", headers);

		ResponseEntity<Map> response = restTemplate.exchange(
				"http://localhost:" + port + "/api/pet", HttpMethod.POST, request, Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(UUID.fromString((String) response.getBody().get("transactionId"))).isNotNull();
		assertThat(response.getBody()).containsEntry("name", "testingPet1").containsEntry("status", "available");
		assertThat(response.getBody()).containsKey("dateCreated");

		wireMockServer.verify(1, com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlEqualTo("/v2/pet")));
	}

	@Test
	void getPet_missingApiKey_returns401() {
		ResponseEntity<Map> response = restTemplate.getForEntity(
				"http://localhost:" + port + "/api/pet/10", Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorHealth_doesNotRequireApiKey() {
		ResponseEntity<Map> response = restTemplate.getForEntity(
				"http://localhost:" + port + "/actuator/health", Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("status", "UP");
	}

	@Test
	void actuatorPrometheus_doesNotRequireApiKey_andExposesMetrics() {
		ResponseEntity<String> response = restTemplate.getForEntity(
				"http://localhost:" + port + "/actuator/prometheus", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotBlank();
	}
}
