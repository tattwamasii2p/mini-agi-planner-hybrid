package com.adam.agri.planner.symbolic.reasoning.llm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Neural embedding space ℝ^d.
 * Mathematical model: d-dimensional vector space with standard operations.
 */
public class NeuralSpace {

    private final int dimension;

    public NeuralSpace(int dimension) {
        this.dimension = dimension;
    }

    public int getDimension() {
        return dimension;
    }

    /**
     * Vector addition: u + v
     */
    public double[] add(double[] u, double[] v) {
        checkDimensions(u, v);
        double[] result = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            result[i] = u[i] + v[i];
        }
        return result;
    }

    /**
     * Scalar multiplication: c · u
     */
    public double[] scale(double c, double[] u) {
        checkDimension(u);
        double[] result = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            result[i] = c * u[i];
        }
        return result;
    }

    /**
     * Dot product: u · v
     */
    public double dot(double[] u, double[] v) {
        checkDimensions(u, v);
        double sum = 0.0;
        for (int i = 0; i < dimension; i++) {
            sum += u[i] * v[i];
        }
        return sum;
    }

    /**
     * Cosine similarity: (u · v) / (||u|| ||v||)
     * Range: [-1, 1]
     */
    public double cosineSimilarity(double[] u, double[] v) {
        double dot = dot(u, v);
        double normU = norm(u);
        double normV = norm(v);
        if (normU == 0 || normV == 0) return 0.0;
        return dot / (normU * normV);
    }

    /**
     * Euclidean norm: ||u||
     */
    public double norm(double[] u) {
        checkDimension(u);
        double sum = 0.0;
        for (double x : u) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }

    /**
     * Normalized vector: u / ||u||
     */
    public double[] normalize(double[] u) {
        double n = norm(u);
        if (n == 0) return new double[dimension];
        return scale(1.0 / n, u);
    }

    /**
     * Euclidean distance: ||u - v||
     */
    public double distance(double[] u, double[] v) {
        checkDimensions(u, v);
        double sum = 0.0;
        for (int i = 0; i < dimension; i++) {
            double diff = u[i] - v[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Squared distance (cheaper, for comparisons).
     */
    public double squaredDistance(double[] u, double[] v) {
        checkDimensions(u, v);
        double sum = 0.0;
        for (int i = 0; i < dimension; i++) {
            double diff = u[i] - v[i];
            sum += diff * diff;
        }
        return sum;
    }

    /**
     * Find nearest neighbor in a set of vectors.
     */
    public Optional<double[]> nearestNeighbor(double[] query, List<double[]> candidates) {
        if (candidates.isEmpty()) return Optional.empty();

        double[] nearest = null;
        double minDist = Double.MAX_VALUE;

        for (double[] candidate : candidates) {
            double dist = squaredDistance(query, candidate);
            if (dist < minDist) {
                minDist = dist;
                nearest = candidate;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /**
     * K-nearest neighbors.
     */
    public List<double[]> kNearestNeighbors(double[] query, List<double[]> candidates, int k) {
        return candidates.stream()
            .sorted((a, b) -> Double.compare(squaredDistance(query, a), squaredDistance(query, b)))
            .limit(k)
            .toList();
    }

    /**
     * Project vector onto subspace.
     */
    public double[] project(double[] u, double[][] basis) {
        double[] projection = new double[dimension];
        for (double[] v : basis) {
            double coeff = dot(u, v) / dot(v, v);
            projection = add(projection, scale(coeff, v));
        }
        return projection;
    }

    /**
     * Average (centroid) of vectors.
     */
    public double[] centroid(List<double[]> vectors) {
        if (vectors.isEmpty()) {
            return new double[dimension];
        }
        double[] sum = new double[dimension];
        for (double[] v : vectors) {
            sum = add(sum, v);
        }
        return scale(1.0 / vectors.size(), sum);
    }

    /**
     * Entropy of softmax distribution.
     * Higher entropy = more uncertainty.
     */
    public double entropy(double[] logits) {
        double[] probs = softmax(logits);
        double entropy = 0.0;
        for (double p : probs) {
            if (p > 0) {
                entropy -= p * Math.log(p);
            }
        }
        return entropy;
    }

    /**
     * Confidence measure: 1 / (1 + entropy).
     * Range: (0, 1], higher = more confident.
     */
    public double confidence(double[] logits) {
        return 1.0 / (1.0 + entropy(logits));
    }

    /**
     * Softmax function.
     */
    private double[] softmax(double[] logits) {
        double max = Arrays.stream(logits).max().orElse(0.0);
        double[] exp = new double[logits.length];
        double sum = 0.0;
        for (int i = 0; i < logits.length; i++) {
            exp[i] = Math.exp(logits[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < logits.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    /**
     * Zero vector.
     */
    public double[] zero() {
        return new double[dimension];
    }

    /**
     * Random unit vector (uniform on sphere).
     */
    public double[] randomUnit() {
        double[] v = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            v[i] = Math.random() * 2 - 1;
        }
        return normalize(v);
    }

    private void checkDimension(double[] u) {
        if (u.length != dimension) {
            throw new IllegalArgumentException(
                "Expected dimension " + dimension + ", got " + u.length);
        }
    }

    private void checkDimensions(double[] u, double[] v) {
        checkDimension(u);
        checkDimension(v);
    }
}
