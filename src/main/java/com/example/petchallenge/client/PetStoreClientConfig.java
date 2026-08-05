package com.example.petchallenge.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class PetStoreClientConfig {

	@Bean
	public RestClientCustomizer petStoreTimeoutCustomizer(
			@Value("${petstore.connect-timeout-ms:3000}") int connectTimeoutMs,
			@Value("${petstore.read-timeout-ms:5000}") int readTimeoutMs) {

		return builder -> {
			SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
			requestFactory.setConnectTimeout(connectTimeoutMs);
			requestFactory.setReadTimeout(readTimeoutMs);
			builder.requestFactory(requestFactory);
		};
	}
}
