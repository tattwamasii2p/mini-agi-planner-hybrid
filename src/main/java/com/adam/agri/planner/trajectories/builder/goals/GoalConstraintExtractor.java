package com.adam.agri.planner.trajectories.builder.goals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.adam.agri.planner.core.constraints.ConstraintSet;
import com.adam.agri.planner.core.constraints.CostConstraint;
import com.adam.agri.planner.core.constraints.RiskConstraint;
import com.adam.agri.planner.core.constraints.TimeConstraint;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.planning.Goal;
import com.adam.agri.planner.planning.UtilityFunction;

/**
 * Extracts goals and constraints from natural language.
 * Converts text descriptions into formal Goal objects and ConstraintSet.
 *
 * Supported patterns:
 * - "I want to achieve X" → Goal with target state
 * - "Send data to server Y" → Goal with action target
 * "Complete within 5 minutes" → TimeConstraint
 * - "Cost less than $100" → CostConstraint
 * - "Risk should be low" → RiskConstraint
 */
public class GoalConstraintExtractor {

    private final List<String> goalTexts;
    private final List<String> constraintTexts;
    private final List<String> desireTexts;

    public GoalConstraintExtractor() {
        this.goalTexts = new ArrayList<>();
        this.constraintTexts = new ArrayList<>();
        this.desireTexts = new ArrayList<>();
    }

    /**
     * Add goal text.
     */
    public GoalConstraintExtractor addGoal(String goalText) {
        goalTexts.add(goalText);
        return this;
    }

    /**
     * Add constraint text.
     */
    public GoalConstraintExtractor addConstraint(String constraintText) {
        constraintTexts.add(constraintText);
        return this;
    }

    /**
     * Add desire/preference text.
     */
    public GoalConstraintExtractor addDesire(String desireText) {
        desireTexts.add(desireText);
        return this;
    }

    /**
     * Extract all goals from added goal texts.
     */
    public List<Goal> extractGoals() {
        List<Goal> goals = new ArrayList<>();
        for (String text : goalTexts) {
            goals.addAll(parseGoal(text));
        }
        return goals;
    }

    /**
     * Extract main goal (single combined goal).
     */
    public Optional<Goal> extractMainGoal() {
        List<Goal> goals = extractGoals();
        if (goals.isEmpty()) {
            return Optional.empty();
        }
        if (goals.size() == 1) {
            return Optional.of(goals.get(0));
        }
        // Combine multiple goals with conjunction
        return Optional.of(combineGoals(goals));
    }

    /**
     * Extract all constraints from added constraint texts.
     */
    public ConstraintSet extractConstraints() {
        ConstraintSet constraints = new ConstraintSet();
        for (String text : constraintTexts) {
            constraints = parseConstraint(text, constraints);
        }
        return constraints;
    }

    /**
     * Extract user desires/preferences.
     */
    public DesireModel extractDesires() {
        DesireModel desires = new DesireModel();
        for (String text : desireTexts) {
            parseDesire(text, desires);
        }
        return desires;
    }

    /**
     * Parse a goal text into one or more goals.
     */
    private List<Goal> parseGoal(String text) {
        List<Goal> goals = new ArrayList<>();
        String lower = text.toLowerCase();

        // Pattern: "deploy X to Y"
        Matcher deployMatcher = Pattern.compile(
            "deploy\\s+(\\w+)\\s+(?:to|on)\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (deployMatcher.find()) {
            String what = deployMatcher.group(1);
            String where = deployMatcher.group(2);
            goals.add(createDeployGoal(what, where, text));
        }

        // Pattern: "move to X" / "go to X"
        Matcher moveMatcher = Pattern.compile(
            "(?:move|go|navigate)\\s+(?:to|towards)?\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (moveMatcher.find()) {
            String destination = moveMatcher.group(1);
            goals.add(createMoveGoal(destination, text));
        }

        // Pattern: "reach X" / "achieve X"
        Matcher reachMatcher = Pattern.compile(
            "(?:reach|achieve|get to)\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (reachMatcher.find()) {
            String target = reachMatcher.group(1);
            goals.add(createReachGoal(target, text));
        }

        // Pattern: "optimize X" / "minimize X" / "maximize X"
        Matcher optimizeMatcher = Pattern.compile(
            "(optimize|minimize|maximize)\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (optimizeMatcher.find()) {
            String action = optimizeMatcher.group(1).toLowerCase();
            String what = optimizeMatcher.group(2);
            goals.add(createOptimizeGoal(action, what, text));
        }

        // Pattern: "send X to Y"
        Matcher sendMatcher = Pattern.compile(
            "send\\s+(\\w+)\\s+to\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (sendMatcher.find()) {
            String what = sendMatcher.group(1);
            String recipient = sendMatcher.group(2);
            goals.add(createSendGoal(what, recipient, text));
        }

        // If no pattern matched, create generic goal
        if (goals.isEmpty()) {
            goals.add(createGenericGoal(text));
        }

        return goals;
    }

    /**
     * Parse constraint text and add to constraint set.
     */
    private ConstraintSet parseConstraint(String text, ConstraintSet existing) {
        String lower = text.toLowerCase();
        TextConstraint tc = new TextConstraint(text, lower);

        // Time constraints: "within X minutes/seconds/hours"
        Matcher timeMatcher = Pattern.compile(
            "(?:within|in|at most)\\s+(\\d+(?:\\.\\d+)?)\\s*(minutes?|mins?|seconds?|secs?|hours?|hrs?|days?)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (timeMatcher.find()) {
            double amount = Double.parseDouble(timeMatcher.group(1));
            String unit = timeMatcher.group(2).toLowerCase();
            double minutes = convertToMinutes(amount, unit);
            existing.addHard(new TimeConstraint(minutes));
        }

        // Cost constraints: "cost less than X" / "under $X" / "budget of X"
        Matcher costMatcher = Pattern.compile(
            "(?:cost|budget|price)?\\s*(?:less than|under|below|at most|<=|â‰¤)?\\s*(?:\\$|â‚¬|Â£)?\\s*(\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (costMatcher.find()) {
            double maxCost = Double.parseDouble(costMatcher.group(1));
            existing.addHard(new CostConstraint(maxCost));
        }

        // Risk constraints: "low risk" / "max risk X"
        Matcher riskMatcher = Pattern.compile(
            "(?:max\\s+)?risk\\s*(?:is)?\\s*(?:<=?|at most)?\\s*(\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (riskMatcher.find()) {
            double maxRisk = Double.parseDouble(riskMatcher.group(1));
            existing.addHard(new RiskConstraint(maxRisk));
        } else if (lower.contains("low risk")) {
            existing.addHard(new RiskConstraint(0.2));
        } else if (lower.contains("high risk")) {
            existing.addSoft(new RiskConstraint(0.5));
        }

        // Resource constraints: "using at most X resources"
        Matcher resourceMatcher = Pattern.compile(
            "(?:use|using)\\s+(?:at most|maximum|max)\\s+(\\d+(?:\\.\\d+)?)\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (resourceMatcher.find()) {
            double amount = Double.parseDouble(resourceMatcher.group(1));
            String resource = resourceMatcher.group(2);
            // Store as metadata since ResourceConstraint needs proper type
        }

        return existing;
    }

    /**
     * Parse desire text and add to desire model.
     */
    private void parseDesire(String text, DesireModel desires) {
        String lower = text.toLowerCase();

        // Preference indicators
        if (lower.contains("prefer")) {
            desires.addPreference("user_preference", text);
        }
        if (lower.contains("if possible")) {
            desires.addSoftConstraint(text);
        }
        if (lower.contains("ideally")) {
            desires.addIdeal(text);
        }

        // Cost preference
        if (lower.contains("cheap") || lower.contains("low cost")) {
            desires.setCostWeight(0.5, 1.0); // minimize cost
        }
        if (lower.contains("fast") || lower.contains("quick")) {
            desires.setTimeWeight(0.5, 1.0); // minimize time
        }
        if (lower.contains("safe") || lower.contains("reliable")) {
            desires.setRiskWeight(0.5, -1.0); // minimize risk
        }
    }

    private double convertToMinutes(double amount, String unit) {
        String u = unit.toLowerCase();
        if (u.startsWith("minute") || u.startsWith("min")) return amount;
        if (u.startsWith("hour") || u.startsWith("hr")) return amount * 60;
        if (u.startsWith("second") || u.startsWith("sec")) return amount / 60;
        if (u.startsWith("day")) return amount * 60 * 24;
        return amount;
    }

    private Goal createDeployGoal(String what, String where, String fullText) {
        StateId target = StateId.of("deployed_" + what + "_on_" + where);
        return new Goal(target, createUtilityFromText(fullText));
    }

    private Goal createMoveGoal(String destination, String fullText) {
        StateId target = StateId.of("at_" + destination);
        return new Goal(target, createUtilityFromText(fullText));
    }

    private Goal createReachGoal(String target, String fullText) {
        StateId targetState = StateId.of("reached_" + target);
        return new Goal(targetState, createUtilityFromText(fullText));
    }

    private Goal createOptimizeGoal(String action, String what, String fullText) {
        StateId target = StateId.of("optimized_" + what);
        // Optimization goals get higher utility weight
        return new Goal(target, (from, to) -> {
            if (to.getTargetState() != null) {
                String id = to.getTargetState().toString();
                if (id.contains("optimized")) return 150.0;
            }
            return 100.0;
        });
    }

    private Goal createSendGoal(String what, String recipient, String fullText) {
        StateId target = StateId.of("sent_" + what + "_to_" + recipient);
        return new Goal(target, createUtilityFromText(fullText));
    }

    private Goal createGenericGoal(String text) {
        StateId target = StateId.of("goal_" + text.hashCode());
        return new Goal(target, createUtilityFromText(text));
    }

    private UtilityFunction createUtilityFromText(String text) {
        // Analyze text for utility hints
        String lower = text.toLowerCase();

        if (lower.contains("urgent") || lower.contains("critical")) {
            return (from, to) -> 200.0; // High utility
        }
        if (lower.contains("important")) {
            return (from, to) -> 150.0;
        }
        if (lower.contains("optional") || lower.contains("if possible")) {
            return (from, to) -> 50.0; // Lower utility
        }

        return (from, to) -> 100.0; // Default
    }

    private Goal combineGoals(List<Goal> goals) {
        // Create compound goal requiring all sub-goals
        // Simplified: just return the highest utility goal
        return goals.stream()
            .max(Comparator.comparingDouble(g -> {
                // Estimate utility (this is simplified)
                return 100.0;
            }))
            .orElse(goals.get(0));
    }

    /**
     * Helper class to hold constraint text.
     */
    private static class TextConstraint {
        final String original;
        final String processed;

        TextConstraint(String original, String processed) {
            this.original = original;
            this.processed = processed;
        }
    }
}
