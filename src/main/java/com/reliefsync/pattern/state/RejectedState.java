package com.reliefsync.pattern.state;

import com.reliefsync.model.enums.RequestStateType;

public final class RejectedState implements ReliefRequestState {
  public RequestStateType type() {
    return RequestStateType.REJECTED;
  }
}
