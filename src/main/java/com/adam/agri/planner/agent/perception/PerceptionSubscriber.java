package com.adam.agri.planner.agent.perception;

import java.util.function.Predicate;

/**
 * Subscriber interface for perception events.
 * Observer pattern for reactive perception handling.
 */
@FunctionalInterface
public interface PerceptionSubscriber {

    /**
     * Handle incoming perception event.
     *
     * @param event The perception event
     * @return true if event was consumed, false to propagate to other subscribers
     */
    boolean onPerception(PerceptionEvent event);

    /**
     * Get subscription filter.
     * Only events passing this filter are delivered.
     */
    default Predicate<PerceptionEvent> getFilter() {
        return e -> true; // Accept all by default
    }

    /**
     * Get subscriber priority (higher = earlier delivery).
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Called when subscription is registered.
     */
    default void onSubscribe(PerceptionSubscription subscription) {}

    /**
     * Called when subscription is cancelled.
     */
    default void onCancel() {}

    /**
     * Subscription handle for managing subscription lifecycle.
     */
    interface PerceptionSubscription {
        void cancel();
        boolean isActive();
    }
}
