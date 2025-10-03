package no.unit.nva.publication.events.handlers.batch.dynamodb;

public record LoadDynamodbResponse(int itemsProcessed, int messagesQueued, String jobType) {}