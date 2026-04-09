package com.adam.agri.planner.trajectories.builder.observations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.adam.agri.planner.symbolic.ontology.upper.Abstract;
import com.adam.agri.planner.symbolic.ontology.upper.Concept;
import com.adam.agri.planner.symbolic.ontology.upper.Entity;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.reasoning.llm.LLMReasoningBridge;

/**
 * Adapter for natural language observations. Uses LLMReasoningBridge to convert
 * text descriptions to ontology entities.
 *
 * Supports three types of NL inputs: - GOAL: User goals and objectives -
 * CONSTRAINTS: Hard and soft constraints - WORLD_DESCRIPTION: Descriptions of
 * the world state
 */
public class NaturalLanguageAdapter implements ObservationSource<NaturalLanguageAdapter.NLObservation> {

	private final List<NLObservation> observations;
	private double customConfidence = -1;
	private LLMReasoningBridge llmBridge;

	/**
	 * Types of natural language observations.
	 */
	public enum NLType {
		GOAL, // What user wants to achieve
		CONSTRAINT, // Hard/soft limits (time, cost, resources)
		WORLD_DESC, // Description of world state
		DESIRE, // Preferences beyond strict goals
		ACTION // Verbal action descriptions
	}

	/**
	 * Natural language observation with type classification.
	 */
	public static class NLObservation {
		private final String text;
		private final NLType type;
		private final double confidence;
		private final Map<String, Object> metadata;

		public NLObservation(String text, NLType type) {
			this(text, type, 0.7, Collections.emptyMap());
		}

		public NLObservation(String text, NLType type, double confidence, Map<String, Object> metadata) {
			this.text = text;
			this.type = type;
			this.confidence = confidence;
			this.metadata = new HashMap<>(metadata);
		}

		public String getText() {
			return text;
		}

		public NLType getType() {
			return type;
		}

		public double getConfidence() {
			return confidence;
		}

		public Map<String, Object> getMetadata() {
			return Collections.unmodifiableMap(metadata);
		}
	}

	public NaturalLanguageAdapter() {
		this.observations = new ArrayList<>();
	}

	public NaturalLanguageAdapter(LLMReasoningBridge bridge) {
		this();
		this.llmBridge = bridge;
	}

	@Override
	public void addObservation(NLObservation observation) {
		observations.add(observation);
	}

	/**
	 * Add natural language text with type.
	 */
	public void addText(String text, NLType type) {
		observations.add(new NLObservation(text, type));
	}

	/**
	 * Add a goal description.
	 */
	public void addGoal(String goalText) {
		addText(goalText, NLType.GOAL);
	}

	/**
	 * Add a constraint description.
	 */
	public void addConstraint(String constraintText) {
		addText(constraintText, NLType.CONSTRAINT);
	}

	/**
	 * Add a world description.
	 */
	public void addWorldDescription(String description) {
		addText(description, NLType.WORLD_DESC);
	}

	/**
	 * Add preferences/desires.
	 */
	public void addDesire(String preferenceText) {
		addText(preferenceText, NLType.DESIRE);
	}

	/**
	 * Add an action description.
	 */
	public void addAction(String actionText) {
		addText(actionText, NLType.ACTION);
	}

	public void setLLMBridge(LLMReasoningBridge bridge) {
		this.llmBridge = bridge;
	}

	@Override
	public List<NLObservation> getObservations() {
		return Collections.unmodifiableList(observations);
	}

	@Override
	public SourceType getSourceType() {
		return SourceType.NATURAL_LANGUAGE;
	}

	@Override
	public double getSourceConfidence() {
		if (customConfidence > 0) {
			return customConfidence;
		}
		// Average confidence across all observations
		if (observations.isEmpty()) {
			return getSourceType().getDefaultConfidence();
		}
		return observations.stream().mapToDouble(NLObservation::getConfidence).average()
				.orElse(getSourceType().getDefaultConfidence());
	}

	public void setCustomConfidence(double confidence) {
		this.customConfidence = Math.max(0, Math.min(1, confidence));
	}

	@Override
	public List<Entity> toEntities() {
		List<Entity> entities = new ArrayList<>();
		if (llmBridge == null) {
			// Create placeholder entities without LLM
			for (NLObservation obs : observations) {
				entities.add(createTextEntity(obs));
			}
			return entities;
		}

		// Use LLM to convert NL to entities
		for (NLObservation obs : observations) {
			entities.addAll(convertWithLLM(obs));
		}
		return entities;
	}

	/**
	 * Get goal observations (for goal extraction).
	 */
	public List<String> getGoalTexts() {
		return observations.stream().filter(o -> o.getType() == NLType.GOAL).map(NLObservation::getText).toList();
	}

	/**
	 * Get constraint observations (for constraint extraction).
	 */
	public List<String> getConstraintTexts() {
		return observations.stream().filter(o -> o.getType() == NLType.CONSTRAINT).map(NLObservation::getText).toList();
	}

	/**
	 * Get world description observations.
	 */
	public List<String> getWorldDescriptionTexts() {
		return observations.stream().filter(o -> o.getType() == NLType.WORLD_DESC).map(NLObservation::getText).toList();
	}

	/**
	 * Get desire/preference observations.
	 */
	public List<String> getDesireTexts() {
		return observations.stream().filter(o -> o.getType() == NLType.DESIRE).map(NLObservation::getText).toList();
	}

	@Override
	public void clear() {
		observations.clear();
	}

	/**
	 * Create a text entity without LLM processing.
	 */
	private Entity createTextEntity(NLObservation obs) {
		return new TextEntity(obs);
	}

	/**
	 * Convert NL observation to entities using LLM reasoning bridge.
	 */
	private List<Entity> convertWithLLM(NLObservation obs) {
		List<Entity> entities = new ArrayList<>();

		// Use approximateSection to get nearest formal type
		// TODO This is a simplified approach - full implementation would parse
		// structured output
		switch (obs.getType()) {
		case GOAL:
			entities.add(new GoalEntity(obs.getText()));
			break;
		case CONSTRAINT:
			entities.add(new ConstraintEntity(obs.getText()));
			break;
		case WORLD_DESC:
			entities.add(new WorldDescEntity(obs.getText()));
			break;
		case DESIRE:
			entities.add(new DesireEntity(obs.getText()));
			break;
		case ACTION:
			entities.add(new ActionDescEntity(obs.getText()));
			break;
		}

		return entities;
	}

	/**
	 * Entity representing a text observation (placeholder before LLM processing).
	 */
	public static class TextEntity extends Abstract {
		private final NLObservation observation;

		public TextEntity(NLObservation observation) {
			super(EntityId.of("nl_" + observation.getText().hashCode()), Set.of());
			this.observation = observation;
		}

		@Override
		public String getName() {
			return observation.getType() + ": "
					+ observation.getText().substring(0, Math.min(50, observation.getText().length()))
					+ (observation.getText().length() > 50 ? "..." : "");
		}

		@Override
		public boolean isCompatibleWith(Entity other) {
			return other instanceof TextEntity;
		}

		@Override
		public boolean isCompatibleWith(Concept other) {
			return other instanceof TextEntity;
		}

		public NLObservation getObservation() {
			return observation;
		}

		@Override
		public String toString() {
			return "TextEntity{" + getName() + "}";
		}
	}

	/**
	 * Entity representing a parsed goal.
	 */
	public static class GoalEntity extends TextEntity {
		public GoalEntity(String text) {
			super(new NLObservation(text, NLType.GOAL));
		}
	}

	/**
	 * Entity representing a parsed constraint.
	 */
	public static class ConstraintEntity extends TextEntity {
		public ConstraintEntity(String text) {
			super(new NLObservation(text, NLType.CONSTRAINT));
		}
	}

	/**
	 * Entity representing a world description.
	 */
	public static class WorldDescEntity extends TextEntity {
		public WorldDescEntity(String text) {
			super(new NLObservation(text, NLType.WORLD_DESC));
		}
	}

	/**
	 * Entity representing a desire/preference.
	 */
	public static class DesireEntity extends TextEntity {
		public DesireEntity(String text) {
			super(new NLObservation(text, NLType.DESIRE));
		}
	}

	/**
	 * Entity representing an action description.
	 */
	public static class ActionDescEntity extends TextEntity {
		public ActionDescEntity(String text) {
			super(new NLObservation(text, NLType.ACTION));
		}
	}
}
