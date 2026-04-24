package org.assimbly.dil.blocks.models;

import dev.langchain4j.model.chat.ChatModel;

public class GoogleAiGeminiChatModel {

    public ChatModel create() {

        return dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName(System.getenv("GEMINI_MODEL_NAME"))
                .build();
    }

}
