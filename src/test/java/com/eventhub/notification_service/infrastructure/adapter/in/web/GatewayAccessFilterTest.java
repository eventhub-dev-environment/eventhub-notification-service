package com.eventhub.notification_service.infrastructure.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAccessFilterTest {

    private GatewayAccessFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAccessFilter();
        ReflectionTestUtils.setField(filter, "expectedSecret", "gateway-secret");
    }

    @Test
    void rejectsRequestsWithoutGatewaySecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, chain(chainCalled));

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void allowsRequestsWithGatewaySecret() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications/me");
        request.addHeader("X-Gateway-Secret", "gateway-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, chain(chainCalled));

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private FilterChain chain(AtomicBoolean chainCalled) {
        return (request, response) -> chainCalled.set(true);
    }
}
