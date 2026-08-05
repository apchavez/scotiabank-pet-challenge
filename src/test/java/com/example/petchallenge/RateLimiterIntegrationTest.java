package com.example.petchallenge;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"spring.cache.type=simple",
		"resilience4j.ratelimiter.instances.petApi.limit-for-period=1",
		"resilience4j.ratelimiter.instances.petApi.limit-refresh-period=10s",
		"resilience4j.ratelimiter.instances.petApi.timeout-duration=0"
})
class RateLimiterIntegrationTest {

	private static final String VALID_API_KEY = "change-me-in-prod";
	private static final String API_KEY_HEADER = "X-Api-Key";

	private static WireMockServer wireMockServer;

	@LocalServerPort
	private int port;

	private final TestRestTemplate restTemplate = new TestRestTemplate();

	@BeforeAll
	static void startWireMock() {
		wireMockServer = new WireMockServer(0);
		wireMockServer.start();
	}

	@AfterAll
	static void stopWireMock() {
		wireMockServer.stop();
	}

	@DynamicPropertySource
	static void petstoreBaseUrl(DynamicPropertyRegistry registry) {
		registry.add("petstore.base-url", () -> "http://localhost:" + wireMockServer.port() + "/v2");
	}

	@Test
	void exceedingRateLimit_returnsTooManyRequests() {
		wireMockServer.stubFor(get(urlEqualTo("/v2/pet/10"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"id\":10,\"name\":\"doggie\",\"status\":\"available\"}")));

		HttpHeaders headers = new HttpHeaders();
		headers.set(API_KEY_HEADER, VALID_API_KEY);
		HttpEntity<Void> entity = new HttpEntity<>(headers);

		ResponseEntity<Map> first = restTemplate.exchange(
				"http://localhost:" + port + "/api/pet/10", HttpMethod.GET, entity, Map.class);
		ResponseEntity<Map> second = restTemplate.exchange(
				"http://localhost:" + port + "/api/pet/10", HttpMethod.GET, entity, Map.class);

		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
	}
}
