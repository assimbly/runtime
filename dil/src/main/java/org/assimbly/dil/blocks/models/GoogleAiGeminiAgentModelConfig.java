package org.assimbly.dil.blocks.models;

import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithoutMemory;
import org.apache.camel.component.langchain4j.agent.api.Guardrails;
import dev.langchain4j.model.chat.ChatLanguageModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAiGeminiAgentModelConfig {

    @Bean("secureAgent")
    public Agent secureAgent(ChatLanguageModel chatLanguageModel) {

        AgentConfiguration config = new AgentConfiguration()
                .withChatModel(chatLanguageModel)
                .withInputGuardrailClasses(Guardrails.defaultInputGuardrails())
                .withOutputGuardrailClasses(Guardrails.defaultOutputGuardrails());

        return new AgentWithoutMemory(config);
    }
}