package com.redis.demos.smarttriage.intake;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * Critic: evaluates customer response quality on a 1-4 rubric.
 * Provides specific feedback for improvement.
 */
@Component
public class ResponseEvaluator {

    private final ChatClient.Builder chatClientBuilder;

    public ResponseEvaluator(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public ResponseEvaluation evaluate(String customerMessage, String draftResponse) {
        var converter = new BeanOutputConverter<>(ResponseEvaluation.class);

        String response = chatClientBuilder.build()
                .prompt()
                .system("""
                    You are a customer service quality evaluator. Score the response on 4 criteria (1 point each):

                    1. EMPATHY: Does the response acknowledge the customer's frustration and show understanding?
                    2. SPECIFICITY: Does it reference specific order details, dates, amounts (not generic)?
                    3. RESOLUTION: Does it offer a concrete resolution with clear next steps and timeline?
                    4. COMPLETENESS: For multi-issue complaints, does it address ALL stated concerns?

                    Return total score (1-4) and specific feedback on what's missing or could be improved.

                    %s
                    """.formatted(converter.getFormat()))
                .user("""
                    Customer message: %s

                    Draft response: %s
                    """.formatted(customerMessage, draftResponse))
                .call()
                .content();

        return converter.convert(response);
    }
}
