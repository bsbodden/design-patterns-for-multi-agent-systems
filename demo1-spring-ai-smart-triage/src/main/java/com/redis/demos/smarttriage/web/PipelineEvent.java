package com.redis.demos.smarttriage.web;

import java.util.List;

public sealed interface PipelineEvent {

    record RoutingEvent(String route, String reasoning, List<ScoreEntry> scores) implements PipelineEvent {}
    record HandoffEvent(String from, String to, String context) implements PipelineEvent {}
    record HandlingEvent(String handler, List<String> tools) implements PipelineEvent {}
    record ToolCallEvent(String tool) implements PipelineEvent {}
    record MemoryEvent(String operation, String detail) implements PipelineEvent {}
    record DraftEvent(String content) implements PipelineEvent {}
    record EvaluationEvent(int score, String feedback, int iteration) implements PipelineEvent {}
    record CompleteEvent(String finalResponse) implements PipelineEvent {}
    record QueryEvent(String text, int index) implements PipelineEvent {}
    record StatusEvent(String phase, String detail) implements PipelineEvent {}
    record ErrorEvent(String message) implements PipelineEvent {}

    record ScoreEntry(String category, double confidence) {}
}
