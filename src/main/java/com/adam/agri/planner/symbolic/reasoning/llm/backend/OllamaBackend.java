package com.adam.agri.planner.symbolic.reasoning.llm.backend;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Ollama backend for local LLM inference.
 *
 * Requires Ollama running locally (default: http://localhost:11434).
 * Supports any model pulled via `ollama pull <model>`.
 */
public class OllamaBackend implements LlmBackend {

    private final String baseUrl;
    private final String model;
    private final int embeddingDimension;

    public OllamaBackend() {
        this("http://localhost:11434", "llama2", 4096);
    }

    public OllamaBackend(String model) {
        this("http://localhost:11434", model, 4096);
    }

    public OllamaBackend(String baseUrl, String model, int embeddingDim) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.embeddingDimension = embeddingDim;
    }

    @Override
    public String complete(String prompt) {
        return complete(prompt, 0.7);
    }

    @Override
    public String complete(String prompt, double temperature) {
        try {
            String requestBody = buildGenerateRequest(prompt, temperature, false);
            String response = sendRequest("/api/generate", requestBody);
            return extractResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Ollama API error", e);
        }
    }

    @Override
    public List<String> generate(String prompt, int n) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            results.add(complete(prompt));
        }
        return results;
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
            String requestBody = buildChatRequest(prompt, systemPrompt, 0.7);
            String response = sendRequest("/api/chat", requestBody);
            return List.of(extractChatResponse(response));
        } catch (IOException e) {
            throw new RuntimeException("Ollama API error", e);
        }
    }

    @Override
    public double[] embed(String text) {
        try {
            String requestBody = String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\"}",
                model, escapeJson(text)
            );
            String response = sendRequest("/api/embeddings", requestBody);
            return parseEmbedding(response);
        } catch (IOException e) {
            // Fallback to deterministic
            return ClaudeBackend.deterministicEmbedding(text, embeddingDimension);
        }
    }

    @Override
    public List<double[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    @Override
    public double classify(String text, String... categories) {
        String prompt = "Classify into one of: " + String.join(", ", categories) +
            "\nText: " + text + "\nCategory:";
        String result = complete(prompt, 0.1).toLowerCase();

        for (String cat : categories) {
            if (result.contains(cat.toLowerCase())) {
                return 1.0;
            }
        }
        return 0.5;
    }

    @Override
    public Map<String, Double> predictTokens(String prefix, int topK) {
        return new HashMap<>(); // Ollama doesn't expose logits
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpURLConnection conn = (HttpURLConnection)
            		URI.create(baseUrl + "/api/tags").toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            return conn.getResponseCode() == 200;
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
        URL url = URI.create(baseUrl + endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

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

    private String buildGenerateRequest(String prompt, double temperature, boolean stream) {
        return String.format(
            "{\"model\":\"%s\",\"prompt\":\"%s\",\"temperature\":%.2f,\"stream\":%b}",
            model, escapeJson(prompt), temperature, stream
        );
    }

    private String buildChatRequest(String prompt, String system, double temperature) {
        return String.format(
            "{\"model\":\"%s\",\"messages\":[" +
            "{\"role\":\"system\",\"content\":\"%s\"}," +
            "{\"role\":\"user\",\"content\":\"%s\"}]," +
            "\"temperature\":%.2f}",
            model, escapeJson(system), escapeJson(prompt), temperature
        );
    }

    private String extractResponse(String json) {
        int start = json.indexOf("\"response\":\"");
        if (start == -1) return "";
        start += 12;
        int end = json.indexOf("\"", start);
        return json.substring(start, end)
            .replace("\\n", "\n")
            .replace("\\\"", "\"");
    }

    private String extractChatResponse(String json) {
        int start = json.indexOf("\"content\":\"");
        if (start == -1) return "";
        start += 11;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private double[] parseEmbedding(String json) {
        int start = json.indexOf("\"embedding\":[");
        if (start == -1) {
            return new double[embeddingDimension];
        }
        start += 12;
        int end = json.indexOf("]", start);
        String[] values = json.substring(start, end).split(",");

        double[] embedding = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            embedding[i] = Double.parseDouble(values[i].trim());
        }
        return embedding;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
