package com.adam.agri.planner.agent.execution;

import com.adam.agri.planner.core.action.Action;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Publisher for actuation events and results.
 * Publishes action execution lifecycle events.
 */
public class ActuationPublisher {

 private final Set<ActuationSubscriber> subscribers;
 private final ExecutorService executor;
 private final boolean asyncDelivery;

 public ActuationPublisher(boolean async) {
 this.subscribers = ConcurrentHashMap.newKeySet();
 this.asyncDelivery = async;
 this.executor = async ? Executors.newCachedThreadPool() : null;
 }

 public ActuationPublisher() {
 this(false);
 }

 /**
 * Subscribe to all actuation events.
 */
 public void subscribe(ActuationSubscriber subscriber) {
 subscribers.add(subscriber);
 }

 /**
 * Subscribe to specific event type.
 */
 public void subscribe(Consumer<ActuationEvent> handler, ActuationEvent.EventType... types) {
 subscribe(new TypedSubscriber(handler, Set.of(types)));
 }

 /**
 * Unsubscribe.
 */
 public void unsubscribe(ActuationSubscriber subscriber) {
 subscribers.remove(subscriber);
 }

 /**
 * Publish action started.
 */
 public void publishActionStarted(Action action, ExecutionContext context) {
 publish(new ActuationEvent(
 ActuationEvent.EventType.ACTION_STARTED,
 action,
 context,
 null,
 null,
 System.currentTimeMillis()
 ));
 }

 /**
 * Publish action completed.
 */
 public void publishActionCompleted(Action action, ExecutionContext context, ExecutionResult result) {
 publish(new ActuationEvent(
 ActuationEvent.EventType.ACTION_COMPLETED,
 action,
 context,
 result,
 null,
 System.currentTimeMillis()
 ));
 }

 /**
 * Publish action failed.
 */
 public void publishActionFailed(Action action, ExecutionContext context, Throwable error) {
 publish(new ActuationEvent(
 ActuationEvent.EventType.ACTION_FAILED,
 action,
 context,
 null,
 error,
 System.currentTimeMillis()
 ));
 }

 /**
 * Publish generic actuation event.
 */
 public void publish(ActuationEvent event) {
 for (ActuationSubscriber sub : subscribers) {
 if (sub.isSubscribedTo(event.getType())) {
 deliver(sub, event);
 }
 }
 }

 /**
 * Publish asynchronously.
 */
 public void publishAsync(ActuationEvent event) {
 if (executor != null) {
 executor.submit(() -> publish(event));
 } else {
 publish(event);
 }
 }

 private void deliver(ActuationSubscriber sub, ActuationEvent event) {
 try {
 if (asyncDelivery && executor != null) {
 executor.submit(() -> sub.onActuation(event));
 } else {
 sub.onActuation(event);
 }
 } catch (Exception e) {
 // Log but don't fail
 }
 }

 /**
 * Get subscriber count.
 */
 public int getSubscriberCount() {
 return subscribers.size();
 }

 /**
 * Clear all subscriptions.
 */
 public void clear() {
 subscribers.clear();
 }

 /**
 * Shutdown executor.
 */
 public void shutdown() {
 if (executor != null) {
 executor.shutdown();
 }
 }

 // Interfaces

 public interface ActuationSubscriber {
 void onActuation(ActuationEvent event);
 default boolean isSubscribedTo(ActuationEvent.EventType type) {
 return true;
 }
 }

 // Typed subscriber implementation
 private static class TypedSubscriber implements ActuationSubscriber {
 private final Consumer<ActuationEvent> handler;
 private final Set<ActuationEvent.EventType> subscribedTypes;

 TypedSubscriber(Consumer<ActuationEvent> handler, Set<ActuationEvent.EventType> types) {
 this.handler = handler;
 this.subscribedTypes = types;
 }

 @Override
 public void onActuation(ActuationEvent event) {
 handler.accept(event);
 }

 @Override
 public boolean isSubscribedTo(ActuationEvent.EventType type) {
 return subscribedTypes.contains(type);
 }
 }

 // Event record
 public record ActuationEvent(
 EventType type,
 Action action,
 ExecutionContext context,
 ExecutionResult result,
 Throwable error,
 long timestamp
 ) {
 public enum EventType {
 ACTION_STARTED,
 ACTION_COMPLETED,
 ACTION_FAILED,
 PRECONDITION_CHECKED,
 SAFETY_VIOLATION,
 ACTUATOR_FEEDBACK
 }
 public EventType getType() { return type; }
 }
}
