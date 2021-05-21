package com.bc.websocket_demo.config;

import com.bc.websocket_demo.service.WebSocketServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 开启WebSocket支持
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public WebSocketServer serverEndpointExporter() {
        return new WebSocketServer();
    }

} 
