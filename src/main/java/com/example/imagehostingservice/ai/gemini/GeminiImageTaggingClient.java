package com.example.imagehostingservice.ai.gemini;

import com.example.imagehostingservice.ai.gemini.config.GeminiProperties;
import com.example.imagehostingservice.exception.ImageTaggingException;
import com.example.imagehostingservice.image.model.ImageTags;
import com.example.imagehostingservice.image.tagging.ImageTaggingClient;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiImageTaggingClient
        implements ImageTaggingClient {

    private static final Schema TEXT_ITEM_SCHEMA =
            Schema.builder()
                    .type(Type.Known.STRING)
                    .minLength(1L)
                    .build();

    private static final Schema COLOR_ITEM_SCHEMA =
            Schema.builder()
                    .type(Type.Known.STRING)
                    .format("enum")
                    .enum_(
                            "white",
                            "black",
                            "grey",
                            "yellow",
                            "red",
                            "blue",
                            "green",
                            "brown",
                            "pink",
                            "orange",
                            "purple"
                    )
                    .build();

    private static final Schema RESPONSE_SCHEMA =
            Schema.builder()
                    .type(Type.Known.OBJECT)
                    .properties(Map.of(
                            "objects",
                            arraySchema(TEXT_ITEM_SCHEMA, 10),
                            "tags",
                            arraySchema(TEXT_ITEM_SCHEMA, 10),
                            "colors",
                            arraySchema(COLOR_ITEM_SCHEMA, 3)
                    ))
                    .required(
                            "objects",
                            "tags",
                            "colors"
                    )
                    .propertyOrdering(
                            "objects",
                            "tags",
                            "colors"
                    )
                    .build();

    private final Client client;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public ImageTags analyze(
            byte[] imageBytes,
            String contentType
    ) {
        Content content = Content.fromParts(
                Part.fromText(ImageTaggingPrompt.TEXT),
                Part.fromBytes(imageBytes, contentType)
        );

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .responseMimeType("application/json")
                        .responseSchema(RESPONSE_SCHEMA)
                        .maxOutputTokens(512)
                        .build();

        try {
            GenerateContentResponse response =
                    client.models.generateContent(
                            properties.getModel(),
                            content,
                            config
                    );

            response.checkFinishReason();

            String responseText = response.text();

            if (responseText == null ||
                    responseText.isBlank()) {
                throw new ImageTaggingException(
                        "Gemini returned an empty response"
                );
            }

            return objectMapper.readValue(
                    responseText,
                    ImageTags.class
            );
        } catch (JacksonException exception) {
            throw new ImageTaggingException(
                    "Could not parse the Gemini response",
                    exception
            );
        } catch (ImageTaggingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ImageTaggingException(
                    "Gemini image tagging failed",
                    exception
            );
        }
    }

    private static Schema arraySchema(
            Schema itemSchema,
            long maximumItems
    ) {
        return Schema.builder()
                .type(Type.Known.ARRAY)
                .items(itemSchema)
                .maxItems(maximumItems)
                .build();
    }
}