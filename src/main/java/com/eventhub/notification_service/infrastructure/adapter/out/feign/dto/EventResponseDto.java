package com.eventhub.notification_service.infrastructure.adapter.out.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDto {
    private String id;
    private String title;
    private String description;
    private String location;
    private EventBrandingDto branding;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventBrandingDto {
        private String logoUrl;
        private String primaryColor;
        private String secondaryColor;
        private String supportEmail;
        private String organizerName;
        private String bannerUrl;
    }
}