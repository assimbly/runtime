package org.assimbly.integrationrest.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;

@Configuration
public class IntegrationConfig {

    @Bean
    public SimpMessageSendingOperations messagingTemplate() {
        return new SimpMessagingTemplate(webSocketMessageChannel());
    }

    @Bean
    public MessageChannel webSocketMessageChannel() {
        return new ExecutorSubscribableChannel();
    }

}
