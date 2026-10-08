package com.eventhub.notification_service.infrastructure.adapter.out.feign;

import com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.ApiResponse;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.EventResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "event-service",
        url = "${services.event-service.url:http://event-service:8083}"
)
public interface EventServiceClient {

    @GetMapping("/api/v1/events/{id}")
    ApiResponse<EventResponseDto> getEventById(
            @PathVariable("id") String id,
            @RequestHeader("X-Gateway-Secret") String gatewaySecret
    );
}