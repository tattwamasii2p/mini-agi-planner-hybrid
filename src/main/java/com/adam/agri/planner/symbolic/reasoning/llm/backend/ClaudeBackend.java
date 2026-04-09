package com.adam.agri.planner.symbolic.reasoning.llm.backend;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Claude API backend using Anthropic SDK.
 *
 * Requires ANTHROPIC_API_KEY environment variable.
 * Supports Claude 3 models: claude-3-opus, claude-3-sonnet, claude-3-haiku.
 */
public class ClaudeBackend implements LlmBackend {

    private static final String API_BASE = "https://api.anthropic.com/v1";
    private final String apiKey;
    private final String model;
    private final int embeddingDimension;

    public ClaudeBackend() {
        this(System.getenv("ANTHROPIC_API_KEY"), "claude-3-sonnet-20240229", 1536);
    }

    public ClaudeBackend(String apiKey) {
        this(apiKey, "claude-3-sonnet-20240229", 1536);
    }

    public ClaudeBackend(String apiKey, String model, int embeddingDim) {
        this.apiKey = apiKey;
        this.model = model;
        this.embeddingDimension = embeddingDim;
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY not set");
        }
    }

    @Override
    public String complete(String prompt) {
        return complete(prompt, 0.7);
    }

    @Override
    public String complete(String prompt, double temperature) {
        try {
            String requestBody = buildRequestBody(prompt, null, temperature, 1, 4096);
            String response = sendRequest("/messages", requestBody);
            return extractContent(response);
        } catch (IOException e) {
            throw new RuntimeException("Claude API error", e);
        }
    }

    @Override
    public List<String> generate(String prompt, int n) {
        return generate(prompt, n, 0.7, 2048);
    }

    @Override
    public List<String> generate(String prompt, int n, double temperature, int maxTokens) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            results.add(complete(prompt, temperature).trim());
        }
        return results;
    }

    @Override
    public List<String> generate(String prompt, String systemPrompt, int n) {
        try {
            String requestBody = buildRequestBodyWithSystem(prompt, systemPrompt, 0.7, n, 2048);
            String response = sendRequest("/messages", requestBody);
            // Parse multiple candidates if API supports, else repeat
            List<String> results = new ArrayList<>();
            results.add(extractContent(response));
            return results;
        } catch (IOException e) {
            throw new RuntimeException("Claude API error", e);
        }
    }

    @Override
    public double[] embed(String text) {
        // Claude doesn't have embedding API directly, use text-embedding-3
        // Return deterministic hash-based embedding for now
        return deterministicEmbedding(text, embeddingDimension);
    }

    @Override
    public List<double[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    @Override
    public double classify(String text, String... categories) {
        if (categories.length < 2) {
            throw new IllegalArgumentException("Need at least 2 categories");
        }
        String prompt = "Classify the following text into exactly one of these categories: " +
            String.join(", ", categories) + "\n\n" +
            "Text: " + text + "\n\n" +
            "Category:";
        String result = complete(prompt, 0.1);
        // Check which category appears in response
        String lower = result.toLowerCase();
        for (String cat : categories) {
            if (lower.contains(cat.toLowerCase())) {
                return 1.0;
            }
        }
        return 0.5; // Uncertain
    }

    @Override
    public Map<String, Double> predictTokens(String prefix, int topK) {
        // Claude API doesn't expose logits
        return new HashMap<>();
    }

    @Override
    public boolean isAvailable() {
        try {
            complete("test", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    private String sendRequest(String endpoint, String requestBody) throws IOException {
        URL url = URI.create(API_BASE + endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP " + responseCode + ": " + readError(conn));
        }

        return readResponse(conn);
    }

    private String buildRequestBody(String prompt, String systemPrompt,
                                     double temperature, int n, int maxTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(model).append("\",");
        if (systemPrompt != null) {
            sb.append("\"system\":\"").append(escapeJson(systemPrompt)).append("\",");
        }
        sb.append("\"messages\":[{\"role\":\"user\",\"content\":\"");
        sb.append(escapeJson(prompt));
        sb.append("\"}],");
        sb.append("\"max_tokens\":").append(maxTokens).append(",");
        sb.append("\"temperature\":").append(temperature);
        sb.append("}");
        return sb.toString();
    }

    private String buildRequestBodyWithSystem(String prompt, String system,
                                              double temperature, int n, int maxTokens) {
        return buildRequestBody(prompt, system, temperature, n, maxTokens);
    }

    private String extractContent(String response) {
        // Parse JSON response
        int start = response.indexOf("\"text\":\"");
        if (start == -1) return "";
        start += 8;
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private String readError(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Deterministic pseudo-embedding for when real API unavailable.
     * Uses hash distribution to create consistent vectors.
     */
    public static double[] deterministicEmbedding(String text, int dim) {
        double[] embedding = new double[dim];
        int seed = text.hashCode();
        java.util.Random random = new java.util.Random(seed);
        for (int i = 0; i < dim; i++) {
            embedding[i] = (random.nextDouble() - 0.5) * 2.0; // [-1, 1]
        }
        // Normalize
        double norm = 0;
        for (double v : embedding) norm += v * v;
        norm = Math.sqrt(norm);
        for (int i = 0; i < dim; i++) embedding[i] /= norm;
        return embedding;
    }
}
