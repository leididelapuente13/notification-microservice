package com.jcaa.notificationservice.application.mapper;

import com.jcaa.notificationservice.application.dto.command.ProcessDomainEventCommand;
import com.jcaa.notificationservice.application.exception.InvalidNotificationEventException;
import com.jcaa.notificationservice.application.model.UserEventType;
import com.jcaa.notificationservice.domain.core.model.EmailDestination;
import com.jcaa.notificationservice.domain.core.vo.UserEmail;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class UserEventEmailMapper {

  private static final String EMAIL_FIELD = "email";
  private static final String NAME_FIELD = "name";
  private static final String AMOUNT_FIELD = "amount";
  private static final String CURRENCY_FIELD = "currency";
  private static final String MISSING_PAYLOAD_MESSAGE = "The notification payload is required.";
  private static final String MISSING_FIELD_MESSAGE =
      "The notification payload is missing a required field.";
  private static final String CREATED_SUBJECT = "Welcome to our platform";
  private static final String UPDATED_SUBJECT = "Your account was updated";
  private static final String FUNDS_DEPOSITED_SUBJECT = "Deposit received successfully";
  private static final String CREATED_BODY_TEMPLATE =
      "<p>Hello %s, your account was created successfully.</p>";
  private static final String UPDATED_BODY_TEMPLATE =
      "<p>Hello %s, your account information was updated successfully.</p>";

  public EmailDestination toEmailDestination(
      final ProcessDomainEventCommand command, final UserEventType eventType) {
    final Map<String, String> payload = requiredPayload(command.payload());
    final String name = requiredField(payload, NAME_FIELD);
    final UserEmail email = new UserEmail(requiredField(payload, EMAIL_FIELD));
    final String subject = subjectFor(eventType);
    final String body = buildBody(eventType, payload, name);
    return new EmailDestination(email, name, subject, body);
  }

  private static Map<String, String> requiredPayload(final Map<String, String> payload) {
    if (Objects.isNull(payload)) {
      throw new InvalidNotificationEventException(MISSING_PAYLOAD_MESSAGE);
    }
    return payload;
  }

  private static String requiredField(final Map<String, String> payload, final String fieldName) {
    final String value = payload.get(fieldName);
    if (Objects.isNull(value) || value.isBlank()) {
      throw new InvalidNotificationEventException(MISSING_FIELD_MESSAGE);
    }
    return value.strip();
  }

  private static String subjectFor(final UserEventType eventType) {
    return switch (eventType) {
      case CREATED -> CREATED_SUBJECT;
      case UPDATED -> UPDATED_SUBJECT;
      case FUNDS_DEPOSITED -> FUNDS_DEPOSITED_SUBJECT;
      case DELETED -> throw new InvalidNotificationEventException(MISSING_FIELD_MESSAGE);
    };
  }

  private static String buildBody(
      final UserEventType eventType, final Map<String, String> payload, final String name) {
    return switch (eventType) {
      case CREATED -> CREATED_BODY_TEMPLATE.formatted(name);
      case UPDATED -> UPDATED_BODY_TEMPLATE.formatted(name);
      case FUNDS_DEPOSITED -> {
        final String amount = payload.get(AMOUNT_FIELD);
        final String currency = payload.get(CURRENCY_FIELD);
        if (amount != null && !amount.isBlank() && currency != null && !currency.isBlank()) {
          yield "<p>Hello %s, a deposit of %s %s was successfully credited to your account.</p>"
              .formatted(name, amount.strip(), currency.strip());
        } else if (amount != null && !amount.isBlank()) {
          yield "<p>Hello %s, a deposit of %s was successfully credited to your account.</p>"
              .formatted(name, amount.strip());
        } else {
          yield "<p>Hello %s, a deposit was successfully credited to your account.</p>"
              .formatted(name);
        }
      }
      case DELETED -> throw new InvalidNotificationEventException(MISSING_FIELD_MESSAGE);
    };
  }
}
