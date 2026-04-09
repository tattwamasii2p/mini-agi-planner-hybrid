package com.adam.agri.planner.symbolic.reasoning.llm.backend;

import java.util.List;
import java.util.Map;

/**
 * Interface for LLM backend operations.
 * Supports multiple providers: Claude, Ollama, OpenAI-compatible.
 */
public interface LlmBackend {

    /**
     * Generate text completion for given prompt.
     */
    String complete(String prompt);

    /**
     * Generate with temperature control.
     */
    String complete(String prompt, double temperature);

    /**
     * Generate multiple completions.
     */
    List<String> generate(String prompt, int n);

    /**
     * Generate with temperature and max tokens.
     */
    List<String> generate(String prompt, int n, double temperature, int maxTokens);

    /**
     * Generate with specific system prompt.
     */
    List<String> generate(String prompt, String systemPrompt, int n);

    /**
     * Generate chain-of-thought reasoning.
     */
    default List<String> generateChain(String prompt, int n) {
        return generate("Reason step by step: " + prompt, n, 0.7, 2048);
    }

    /**
     * Get embedding vector for text.
     */
    double[] embed(String text);

    /**
     * Batch embedding for multiple texts.
     */
    List<double[]> embedBatch(List<String> texts);

    /**
     * Classify text into categories.
     * Returns confidence score (0.0 to 1.0).
     */
    double classify(String text, String... categories);

    /**
     * Predict next token probabilities.
     */
    Map<String, Double> predictTokens(String prefix, int topK);

    /**
     * Check if backend is available.
     */
    boolean isAvailable();

    /**
     * Get model name.
     */
    String getModel();

    /**
     * Get embedding dimension.
     */
    int getEmbeddingDimension();
}
