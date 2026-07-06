package org.assimbly.dil.blocks.models;

import java.time.Duration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithoutMemory;

public class GoogleAiGeminiAgent {

    public static Agent create() {

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName(System.getenv("GEMINI_MODEL_NAME"))
                .timeout(Duration.ofSeconds(10))
                .build();

        AgentConfiguration config = new AgentConfiguration()
                .withChatModel(model);

        return new AgentWithoutMemory(config);
    }
}
