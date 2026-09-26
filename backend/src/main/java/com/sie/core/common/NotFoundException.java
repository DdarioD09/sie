package com.sie.core.common;

/** Thrown by any service when a lookup by id fails. Mapped to HTTP 404 by {@link GlobalExceptionHandler}. */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }

  public static NotFoundException forEntity(String entityName, Object id) {
    return new NotFoundException(entityName + " with id " + id + " was not found");
  }
}
