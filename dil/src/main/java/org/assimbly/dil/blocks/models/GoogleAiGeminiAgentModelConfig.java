package org.assimbly.dil.blocks.models;

import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithoutMemory;
import dev.langchain4j.model.chat.ChatModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAiGeminiAgentModelConfig {

    @Bean("secureAgent")
    public Agent secureAgent(ChatModel chatModel) {

        AgentConfiguration config = new AgentConfiguration()
                .withChatModel(chatModel);

        return new AgentWithoutMemory(config);
    }
}