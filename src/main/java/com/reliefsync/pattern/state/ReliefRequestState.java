package com.reliefsync.pattern.state;

import com.reliefsync.model.enums.RequestStateType;

public interface ReliefRequestState {
  RequestStateType type();

  default void submit(ReliefRequestContext c) {
    throw c.invalidTransition("submit");
  }

  default void beginVerification(ReliefRequestContext c) {
    throw c.invalidTransition("begin verification");
  }

  default void markVerified(ReliefRequestContext c) {
    throw c.invalidTransition("mark verified");
  }

  default void allocate(ReliefRequestContext c) {
    throw c.invalidTransition("allocate");
  }

  default void dispatch(ReliefRequestContext c) {
    throw c.invalidTransition("dispatch");
  }

  default void deliver(ReliefRequestContext c) {
    throw c.invalidTransition("deliver");
  }

  default void returnForCorrection(ReliefRequestContext c) {
    throw c.invalidTransition("return for correction");
  }

  default void reject(ReliefRequestContext c) {
    throw c.invalidTransition("reject");
  }

  default void cancel(ReliefRequestContext c) {
    throw c.invalidTransition("cancel");
  }
}
