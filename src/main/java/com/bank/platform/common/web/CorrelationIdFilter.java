package com.bank.platform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String inbound = request.getHeader(CorrelationId.HEADER_NAME);
            if (inbound != null && !inbound.isBlank()) {
                CorrelationId.set(inbound);
            } else {
                CorrelationId.getOrCreate();
            }

            // Always return correlation id
            response.setHeader(CorrelationId.HEADER_NAME, CorrelationId.getOrCreate());

            filterChain.doFilter(request, response);
        } finally {
            CorrelationId.clear();
        }
    }
}