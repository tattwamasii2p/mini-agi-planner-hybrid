package com.adam.agri.planner.symbolic.reasoning.llm;

import com.adam.agri.planner.sheaf.Sheaf;

/**
 * Neural Sheaf Section (Layer 10): Probabilistic local section.
 *
 * Implements Sheaf.LocalSection with embedding-based confidence.
 * Represents "soft" knowledge: belief not yet formalized as proof.
 *
 * Modal interpretation: This section carries ◇ (possibility) modality.
 */
public class NeuralSheafSection<T> implements Sheaf.LocalSection<T> {

    private final T element;
    private final double[] embedding;
    private final double confidence;
    private final NeuralSpace space;

    /**
     * Create neural section with embedding-based confidence.
     *
     * Confidence = 1 / (1 + entropy), where entropy is computed
     * from softmax over embedding dimensions.
     *
     * @param element The formal element this section approximates
     * @param embedding LLM embedding vector
     * @param space Neural space for operations
     */
    public NeuralSheafSection(T element, double[] embedding, NeuralSpace space) {
        this.element = element;
        this.embedding = embedding.clone();
        this.space = space;
        this.confidence = space.confidence(embedding);
    }

    @Override
    public T getElement() {
        return element;
    }

    @Override
    public boolean isExact() {
        return false; // Neural sections are approximate
    }

    /**
     * Get embedding vector.
     */
    public double[] getEmbedding() {
        return embedding.clone();
    }

    /**
     * Modal confidence: P_LLM(element) as belief in section validity.
     * Range: (0, 1]
     * Higher = more confident in this section's validity.
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Check if this section is "possible" (◊) per modal operator.
     * Threshold: confidence > 0.7
     */
    public boolean isPossible() {
        return confidence > 0.7;
    }

    /**
     * Check if this section should be treated as "probable".
     */
    public boolean isProbable() {
        return confidence > 0.9;
    }

    /**
     * Compute gluing compatibility with another section.
     * Two neural sections are compatible if their embeddings are close.
     *
     * For exact sections: exact match or formal compatibility
     * For approximate sections: embedding similarity > threshold
     */
    @Override
    public boolean isCompatibleWith(Sheaf.LocalSection<T> other) {
        if (other instanceof NeuralSheafSection) {
            NeuralSheafSection<T> neural = (NeuralSheafSection<T>) other;
            double similarity = space.cosineSimilarity(embedding, neural.embedding);
            return similarity > 0.85; // Threshold for neural compatibility
        }
        // With exact sections: check formal compatibility
        return other.getElement().equals(element);
    }

    /**
     * Try to glue with another section.
     * Returns merged section if compatible, empty otherwise.
     */
    @Override
    public java.util.Optional<Sheaf.LocalSection<T>> glueWith(Sheaf.LocalSection<T> other) {
        if (!isCompatibleWith(other)) {
            return java.util.Optional.empty();
        }

        if (other instanceof NeuralSheafSection) {
            NeuralSheafSection<T> neural = (NeuralSheafSection<T>) other;
            // Average embeddings
            double[] merged = space.centroid(java.util.List.of(embedding, neural.embedding));
            // Take element with higher confidence
            T mergedElement = confidence > neural.confidence ? element : neural.element;
            return java.util.Optional.of(
                new NeuralSheafSection<>(mergedElement, merged, space)
            );
        }

        // Cannot glue exact with approximate meaningfully
        return java.util.Optional.empty();
    }

    /**
     * Refine this section with additional information.
     * Incorporate new embedding to narrow distribution.
     */
    public NeuralSheafSection<T> refine(double[] additionalEmbedding, double weight) {
        double[] refined = new double[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            double a = embedding[i];
            double b = i < additionalEmbedding.length ? additionalEmbedding[i] : 0;
            refined[i] = (1 - weight) * a + weight * b;
        }
        return new NeuralSheafSection<>(element, refined, space);
    }

    /**
     * Distance to another neural section in embedding space.
     */
    public double distanceTo(NeuralSheafSection<T> other) {
        return space.distance(embedding, other.embedding);
    }

    /**
     * Similarity score to another section.
     */
    public double similarityTo(NeuralSheafSection<T> other) {
        return space.cosineSimilarity(embedding, other.embedding);
    }

    @Override
    public String toString() {
        return String.format("NeuralSection[%s confidence=%.3f]", element, confidence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof NeuralSheafSection) {
            NeuralSheafSection<?> that = (NeuralSheafSection<?>) o;
            return element.equals(that.element) &&
                   java.util.Arrays.equals(embedding, that.embedding);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return element.hashCode() + java.util.Arrays.hashCode(embedding);
    }
}
