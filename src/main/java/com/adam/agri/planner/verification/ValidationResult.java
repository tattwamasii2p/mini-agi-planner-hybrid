package com.adam.agri.planner.verification;

import com.adam.agri.planner.logic.Proof;

import java.util.Objects;
import java.util.Optional;

/**
 * Verification result for plan-as-proof validation.
 *
 * A validation result indicates whether a trajectory successfully represents
 * a proof of the goal proposition, with optional proof witness.
 *
 * @param valid true if the plan is a valid proof
 * @param message human-readable description
 * @param proof optional proof witness (Curry-Howard term)
 */
public record ValidationResult(boolean valid, String message, Optional<Proof> proof) {

    /**
     * Factory for successful validation.
     */
    public static ValidationResult success(String message, Proof proof) {
        Objects.requireNonNull(proof);
        return new ValidationResult(true, message, Optional.of(proof));
    }

    /**
     * Factory for successful validation without proof extraction.
     */
    public static ValidationResult success(String message) {
        return new ValidationResult(true, message, Optional.empty());
    }

    /**
     * Factory for failed validation.
     */
    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message, Optional.empty());
    }

    /**
     * Factory for failed validation with diagnostic info.
     */
    public static ValidationResult failure(String message, int stepIndex, String stepInfo) {
        return new ValidationResult(false,
            String.format("%s at step %d: %s", message, stepIndex, stepInfo),
            Optional.empty());
    }

    /**
     * Check if validation passed.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Get error message for failed validation.
     */
    public String getError() {
        return valid ? null : message;
    }

    /**
     * Convert to boolean for conditional checks.
     */
    public boolean toBoolean() {
        return valid;
    }

    @Override
    public String toString() {
        return String.format("ValidationResult{valid=%s, message='%s', hasProof=%s}",
            valid, message, proof.isPresent());
    }
}
