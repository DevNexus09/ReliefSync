package com.reliefsync.model.verification;

import com.reliefsync.exception.ValidationException;

public record VerificationPolicyConfig(long highQuantityThreshold, long criticalQuantityThreshold) {
  public VerificationPolicyConfig {
    if (highQuantityThreshold <= 0 || criticalQuantityThreshold < highQuantityThreshold)
      throw new ValidationException("Verification thresholds are invalid.");
  }

  public static VerificationPolicyConfig defaults() {
    return new VerificationPolicyConfig(500, 1000);
  }
}
