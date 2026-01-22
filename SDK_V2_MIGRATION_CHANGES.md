# AWS SDK v2 Migration Changes

This document describes the changes made to fix issues related to the AWS SDK v1 to v2 migration in the nva-publication-api project.

## Overview

The migration from AWS SDK v1 to v2 introduced several breaking changes due to:
1. Immutable objects in SDK v2 (vs mutable in v1)
2. Different JSON serialization behavior of `AttributeValue` objects
3. Different null handling (empty collections instead of null)
4. Two different `AttributeValue` types that need conversion between each other

## Changes by Category

### 1. DynamoDB Local Dependency Upgrade

**File:** `gradle/libs.versions.toml`

Updated the DynamoDB Local dependency to support SDK v2:
- Changed from `com.amazonaws:DynamoDBLocal:2.0.0` to `software.amazon.dynamodb:DynamoDBLocal:3.1.0`
- The new version provides `DynamoDBEmbedded.create(null, true).dynamoDbClient()` which returns an SDK v2 `DynamoDbClient` directly

**Files:**
- `publication-testing/src/main/java/no/unit/nva/publication/service/ResourcesLocalTest.java`
- `publication-rest/src/test/java/no/unit/nva/publication/download/utils/UriShortenerLocalDynamoDb.java`

Updated to use the new DynamoDB Local embedded client pattern.

---

### 2. ScanDatabaseRequest JSON Serialization Fix

**Problem:** The `ScanDatabaseRequest` class stored `Map<String, AttributeValue>` for the pagination start marker. SDK v2's `AttributeValue` doesn't serialize/deserialize properly with Jackson, causing an infinite loop in batch scanning because the start marker was corrupted after JSON round-trip.

**Solution:** Store `Map<String, String>` internally and convert to/from `AttributeValue` only when needed.

**File:** `publication-event-handlers/src/main/java/no/unit/nva/publication/events/bodies/ScanDatabaseRequest.java`

Changes:
- Changed `startMarker` field from `Map<String, AttributeValue>` to `Map<String, String>`
- Added `getExclusiveStartKey()` method to convert to `Map<String, AttributeValue>` for DynamoDB operations
- Added `toAttributeValueMap()` and `toStringMap()` helper methods
- Updated builder to accept `Map<String, AttributeValue>` and convert internally

**File:** `publication-event-handlers/src/main/java/no/unit/nva/publication/events/handlers/batch/EventBasedBatchScanHandler.java`

Changes:
- Updated to use `input.getExclusiveStartKey()` instead of `input.getStartMarker()` when calling `resourceService.scanResources()`

**File:** `publication-event-handlers/src/test/java/no/unit/nva/publication/events/handlers/batch/EventBasedBatchScanHandlerTest.java`

Changes:
- Updated `getLatestEmittedStartingPoint()` to use `getExclusiveStartKey()` instead of `getStartMarker()`

---

### 3. AttributeValue Type Conversion

**Problem:** Two different `AttributeValue` classes exist:
- `com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue` (Lambda events)
- `software.amazon.awssdk.services.dynamodb.model.AttributeValue` (SDK v2)

Code was trying to convert between them using JSON serialization, which doesn't work because SDK v2's `AttributeValue` lacks proper Jackson support.

**Solution:** Implement direct type conversion methods.

**File:** `publication-event-handlers/src/main/java/no/unit/nva/publication/events/handlers/fanout/DynamodbStreamRecordDaoMapper.java`

Changes:
- Replaced JSON-based `fromEventMapToDynamodbMap()` with direct conversion
- Added `toSdkAttributeValue()` method that converts Lambda events `AttributeValue` to SDK v2 `AttributeValue` using a switch on the value type
- Removed `throws JsonProcessingException` from method signatures (no longer needed)

**File:** `publication-event-handlers/src/test/java/no/unit/nva/publication/events/handlers/dynamodbstream/DynamodbStreamToEventBridgeHandlerTest.java`

Changes:
- Replaced JSON-based `publicationDynamoDbFormat()` with direct conversion
- Added `toLambdaAttributeValueMap()` and `toLambdaAttributeValue()` methods that convert SDK v2 `AttributeValue` to Lambda events `AttributeValue`

**File:** `publication-event-handlers/src/test/java/no/unit/nva/publication/events/handlers/dynamodbstream/DynamoDbEventTestFactory.java`

Changes:
- Same pattern as above - replaced JSON serialization with direct type conversion

---

### 4. SDK v2 Null Handling Differences

**Problem:** In SDK v2, calling `.m()` on an `AttributeValue` that is not a map type returns an empty map `{}` instead of `null`.

**File:** `publication-commons/src/test/java/no/unit/nva/publication/service/impl/MigrationTests.java`

Changes:
- Changed assertion from `assertThat(doiAttributeValue.get("data").m(), is(nullValue()))` to `assertThat(doiAttributeValue.get("data").hasM(), is(false))`

---

### 5. SDK v2 Immutability Fix

**Problem:** SDK v2 objects are immutable. The `applyDeleteResourceConditions()` method was creating a new `Delete` object with conditions but never returning it, so the conditions were silently ignored. This caused `deleteDraftPublicationThrowsExceptionWhenResourceIsPublished` test to fail.

**File:** `publication-commons/src/main/java/no/unit/nva/publication/service/impl/ResourceService.java`

Changes:
- Changed `applyDeleteResourceConditions()` from `void` to return `TransactWriteItem`
- Method now returns a new `TransactWriteItem` with the conditioned `Delete`
- Updated `deleteResourceTransactionItems()` to use the returned value

---

### 6. HTTP Status Code Standardization

**Problem:** Code was using HTTP status constants from various libraries (Google, AWS SDK).

**Solution:** Standardize on `java.net.HttpURLConnection` constants.

**Files:**
- `publication-commons/src/main/java/no/unit/nva/publication/external/services/ChannelClaimClient.java`
- `tickets/src/test/java/no/unit/nva/publication/ticket/delete/DeleteTicketHandlerTest.java`

Changes:
- Replaced `software.amazon.awssdk.http.HttpStatusCode.FORBIDDEN` with `java.net.HttpURLConnection.HTTP_FORBIDDEN`
- Replaced `software.amazon.awssdk.http.HttpStatusCode.NOT_FOUND` with `java.net.HttpURLConnection.HTTP_NOT_FOUND`
- Replaced `software.amazon.awssdk.http.HttpStatusCode.OK` with `java.net.HttpURLConnection.HTTP_OK`

---

## Type Conversion Reference

### Lambda Events AttributeValue to SDK v2 AttributeValue

```java
private static software.amazon.awssdk.services.dynamodb.model.AttributeValue toSdkAttributeValue(
        AttributeValue value) {
    if (value.getS() != null) {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(value.getS()).build();
    }
    if (value.getN() != null) {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n(value.getN()).build();
    }
    if (value.getB() != null) {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                   .b(SdkBytes.fromByteBuffer(value.getB())).build();
    }
    if (value.getM() != null && !value.getM().isEmpty()) {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                   .m(fromEventMapToDynamodbMap(value.getM())).build();
    }
    if (value.getL() != null && !value.getL().isEmpty()) {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                   .l(value.getL().stream().map(this::toSdkAttributeValue).collect(Collectors.toList())).build();
    }
    if (value.getBOOL() != null) {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(value.getBOOL()).build();
    }
    if (value.getNULL() != null && value.getNULL()) {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().nul(true).build();
    }
    // ... handle other types
}
```

### SDK v2 AttributeValue to Lambda Events AttributeValue

```java
private static AttributeValue toLambdaAttributeValue(
        software.amazon.awssdk.services.dynamodb.model.AttributeValue value) {
    return switch (value.type()) {
        case S -> new AttributeValue().withS(value.s());
        case N -> new AttributeValue().withN(value.n());
        case B -> new AttributeValue().withB(value.b().asByteBuffer());
        case M -> new AttributeValue().withM(toLambdaAttributeValueMap(value.m()));
        case L -> new AttributeValue().withL(value.l().stream()
                      .map(this::toLambdaAttributeValue).collect(Collectors.toList()));
        case BOOL -> new AttributeValue().withBOOL(value.bool());
        case NUL -> new AttributeValue().withNULL(value.nul());
        // ... handle other types
    };
}
```

---

## Testing

All 6176+ tests in `publication-commons` and `publication-event-handlers` pass after these changes.
