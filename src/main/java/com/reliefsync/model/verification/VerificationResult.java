package com.reliefsync.model.verification;

public record VerificationResult(
    VerificationOutcome outcome,
    VerificationLevel processedLevel,
    VerificationLevel nextRequiredLevel,
    boolean verificationComplete) {}
