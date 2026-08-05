package com.example.petchallenge.security;

import com.example.petchallenge.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
	private static final String API_KEY_HEADER = "X-Api-Key";

	private final String expectedApiKey;
	private final ObjectMapper objectMapper;

	public ApiKeyAuthFilter(@Value("${app.api-key}") String expectedApiKey, ObjectMapper objectMapper) {
		this.expectedApiKey = expectedApiKey;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String providedApiKey = request.getHeader(API_KEY_HEADER);
		if (providedApiKey == null || !providedApiKey.equals(expectedApiKey)) {
			log.warn("Petición rechazada por API key inválida o ausente -> {} {}",
					request.getMethod(), request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write(objectMapper.writeValueAsString(
					ErrorResponse.of("API key inválida o ausente")));
			return;
		}

		log.debug("API key válida -> {} {}", request.getMethod(), request.getRequestURI());
		filterChain.doFilter(request, response);
	}
}
