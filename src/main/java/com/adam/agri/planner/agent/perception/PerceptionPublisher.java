package com.adam.agri.planner.agent.perception;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * Publisher for perception events with subscription management.
 * Implements pub/sub pattern for perception distribution.
 */
public class PerceptionPublisher {

    private final Set<Subscription> subscribers;
    private final ExecutorService executor;
    private final boolean asyncDelivery;

    public PerceptionPublisher(boolean async) {
        this.subscribers = ConcurrentHashMap.newKeySet();
        this.asyncDelivery = async;
        this.executor = async ? Executors.newCachedThreadPool() : null;
    }

    public PerceptionPublisher() {
        this(false);
    }

    /**
     * Subscribe to perception events.
     */
    public PerceptionSubscriber.PerceptionSubscription subscribe(PerceptionSubscriber subscriber) {
        Subscription sub = new Subscription(subscriber);
        subscribers.add(sub);
        subscriber.onSubscribe(sub);
        return sub;
    }

    /**
     * Subscribe with custom filter.
     */
    public PerceptionSubscriber.PerceptionSubscription subscribe(
            PerceptionSubscriber subscriber,
            Predicate<PerceptionEvent> filter) {
        Subscription sub = new Subscription(subscriber, filter);
        subscribers.add(sub);
        subscriber.onSubscribe(sub);
        return sub;
    }

    /**
     * Publish event to all matching subscribers.
     */
    public boolean publish(PerceptionEvent event) {
        if (subscribers.isEmpty()) {
            return false;
        }

        // Sort by priority
        List<Subscription> sorted = subscribers.stream()
            .filter(s -> s.isActive())
            .filter(s -> s.matches(event))
            .sorted(Comparator.comparingInt(s -> -s.getPriority()))
            .toList();

        boolean consumed = false;
        for (Subscription sub : sorted) {
            if (consumed) break; // Event consumed, stop propagation

            boolean handled = deliver(sub, event);
            if (handled) {
                consumed = true;
            }
        }

        return consumed;
    }

    /**
     * Publish asynchronously.
     */
    public void publishAsync(PerceptionEvent event) {
        if (executor != null) {
            executor.submit(() -> publish(event));
        } else {
            publish(event);
        }
    }

    /**
     * Get subscriber count.
     */
    public int getSubscriberCount() {
        return (int) subscribers.stream().filter(Subscription::isActive).count();
    }

    /**
     * Clear all subscriptions.
     */
    public void clear() {
        subscribers.forEach(s -> s.subscriber.onCancel());
        subscribers.clear();
    }

    private boolean deliver(Subscription sub, PerceptionEvent event) {
        try {
            if (asyncDelivery && executor != null) {
                return executor.submit(() -> sub.deliver(event)).get();
            } else {
                return sub.deliver(event);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private class Subscription implements PerceptionSubscriber.PerceptionSubscription {
        final PerceptionSubscriber subscriber;
        final Predicate<PerceptionEvent> filter;
        volatile boolean active = true;

        Subscription(PerceptionSubscriber sub) {
            this(sub, sub.getFilter());
        }

        Subscription(PerceptionSubscriber sub, Predicate<PerceptionEvent> filter) {
            this.subscriber = sub;
            this.filter = filter;
        }

        boolean matches(PerceptionEvent event) {
            return active && filter.test(event);
        }

        boolean deliver(PerceptionEvent event) {
            if (!active) return false;
            return subscriber.onPerception(event);
        }

        int getPriority() {
            return subscriber.getPriority();
        }

        @Override
        public void cancel() {
            active = false;
            subscribers.remove(this);
            subscriber.onCancel();
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
