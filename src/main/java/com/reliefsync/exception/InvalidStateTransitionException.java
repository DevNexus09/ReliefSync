package com.reliefsync.exception;

public class InvalidStateTransitionException extends BusinessRuleException {
  public InvalidStateTransitionException(String message) {
    super(message);
  }
}
