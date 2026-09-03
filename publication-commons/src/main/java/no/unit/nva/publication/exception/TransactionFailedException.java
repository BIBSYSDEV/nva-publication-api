package no.unit.nva.publication.exception;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;

public class TransactionFailedException extends RuntimeException {

  private static final String ERROR_MESSAGE =
      "Conflict: This error is thrown when the transaction could not be "
          + "completed. In most cases this is because uniqueness conditions did "
          + "not hold (Typically a duplicate DoiRequest or PublishingRequest)";
  private static final String NO_REASON_CODE = "None";
  private static final String NO_CONDITION = "NO_CONDITION";
  private static final String UNKNOWN_OPERATION = "UNKNOWN_OPERATION";
  private static final String FAILURE_MESSAGE_DELIMITER = "; ";
  private static final Logger logger = LoggerFactory.getLogger(TransactionFailedException.class);
  private static final String PUT = "PUT";
  private static final String UPDATE = "UPDATE";
  private static final String DELETE = "DELETE";
  private static final String CONDITION_CHECK = "CONDITION_CHECK";
  private static final String FAILURE_LOG_MESSAGE =
      "Operation: %s with condition %s for item %s failed with code %s and message %s";

  public TransactionFailedException(Exception exception) {
    super(ERROR_MESSAGE, exception);
  }

  public TransactionFailedException(Exception exception, TransactWriteItemsRequest request) {
    super(ERROR_MESSAGE, exception);
    if (exception instanceof TransactionCanceledException transactionCanceledException) {
      logger.error(constructErrorMessage(transactionCanceledException, request), exception);
    } else {
      logger.error(ERROR_MESSAGE, exception);
    }
  }

  private static String constructErrorMessage(
      TransactionCanceledException exception, TransactWriteItemsRequest request) {
    var reasons = exception.cancellationReasons();
    var items = request.transactItems();
    var failureMessages = new ArrayList<String>();

    for (int i = 0; i < reasons.size(); i++) {
      var reason = reasons.get(i);
      if (didNotFail(reason)) {
        continue;
      }
      var item = i < items.size() ? items.get(i) : null;

      var failedItem = request.transactItems().get(i).toString();
      failureMessages.add(
          FAILURE_LOG_MESSAGE.formatted(
              getOperation(item), getCondition(item), failedItem, reason.code(), reason.message()));
    }

    return failureMessages.isEmpty()
        ? exception.getMessage()
        : String.join(FAILURE_MESSAGE_DELIMITER, failureMessages);
  }

  private static boolean didNotFail(CancellationReason reason) {
    return isNull(reason.code()) || NO_REASON_CODE.equals(reason.code());
  }

  private static String getOperation(TransactWriteItem item) {
    if (isNull(item)) {
      return UNKNOWN_OPERATION;
    } else if (nonNull(item.put())) {
      return PUT;
    } else if (nonNull(item.update())) {
      return UPDATE;
    } else if (nonNull(item.delete())) {
      return DELETE;
    } else if (nonNull(item.conditionCheck())) {
      return CONDITION_CHECK;
    } else {
      return UNKNOWN_OPERATION;
    }
  }

  private static String getCondition(TransactWriteItem item) {
    return Optional.ofNullable(item)
        .flatMap(TransactionFailedException::extractCondition)
        .orElse(NO_CONDITION);
  }

  private static Optional<String> extractCondition(TransactWriteItem item) {
    return Optional.ofNullable(item.put())
        .map(Put::conditionExpression)
        .or(() -> Optional.ofNullable(item.update()).map(Update::conditionExpression))
        .or(() -> Optional.ofNullable(item.delete()).map(Delete::conditionExpression))
        .or(
            () ->
                Optional.ofNullable(item.conditionCheck())
                    .map(ConditionCheck::conditionExpression));
  }
}
