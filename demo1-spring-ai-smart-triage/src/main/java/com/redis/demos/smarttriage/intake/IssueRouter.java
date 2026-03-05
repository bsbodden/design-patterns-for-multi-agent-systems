package com.redis.demos.smarttriage.intake;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * Routes customer issues using LLM structured output.
 * Returns confidence scores across 4 categories.
 */
@Component
public class IssueRouter {

    private final ChatClient.Builder chatClientBuilder;

    public IssueRouter(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public IssueClassification classify(String customerMessage) {
        var converter = new BeanOutputConverter<>(IssueClassification.class);

        String response = chatClientBuilder.build()
                .prompt()
                .system("""
                    You are a customer issue classifier for an e-commerce platform.
                    Classify the customer message into confidence scores across 4 categories.
                    The scores must sum to approximately 1.0.

                    Categories:
                    - deliveryIssue: late delivery, missing package, tracking problems
                    - productQuality: product doesn't match description, defective, damaged
                    - paymentProblem: refund request, billing issue, payment dispute
                    - generalQuestion: general inquiry, how-to question, account issue

                    %s
                    """.formatted(converter.getFormat()))
                .user(customerMessage)
                .call()
                .content();

        return converter.convert(response);
    }
}
