package com.adam.agri.planner.symbolic.reasoning.llm.backend;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * OpenAI-compatible API backend for LLM inference.
 *
 * Supports any OpenAI-compatible API endpoint including:
 * - OpenAI API (default)
 * - vLLM
 * - text-generation-inference (TGI)
 * - LocalAI
 * - and other compatible servers
 *
 * Requires API key via OPENAI_API_KEY environment variable or constructor.
 * Configurable base URL for custom endpoints.
 */
public class OpenAICompatibleBackend implements LlmBackend {

    private static final String DEFAULT_API_BASE = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final int DEFAULT_EMBEDDING_DIM = 1536;

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int embeddingDimension;

    public OpenAICompatibleBackend() {
        this(System.getenv("OPENAI_API_KEY"), DEFAULT_MODEL, DEFAULT_EMBEDDING_DIM);
    }

    public OpenAICompatibleBackend(String apiKey) {
        this(apiKey, DEFAULT_MODEL, DEFAULT_EMBEDDING_DIM);
    }

    public OpenAICompatibleBackend(String apiKey, String model, int embeddingDim) {
        this(DEFAULT_API_BASE, apiKey, model, embeddingDim);
    }

    public OpenAICompatibleBackend(String baseUrl, String apiKey, String model, int embeddingDim) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.embeddingDimension = embeddingDim;
    }

    public String complete(String prompt) {
        return complete(prompt, 0.7);
    }

    public String complete(String prompt, double temperature) {
        try {
            String requestBody = buildChatRequest(prompt, null, temperature, 4096);
            String response = sendRequest("/chat/completions", requestBody);
            return extractChatContent(response);
        } catch (IOException e) {
            throw new RuntimeException("OpenAI-compatible API error", e);
        }
    }

    public List<String> generate(String prompt, int n) {
        return generate(prompt, n, 0.7, 2048);
    }

    public List<String> generate(String prompt, int n, double temperature, int maxTokens) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            results.add(complete(prompt, temperature).trim());
        }
        return results;
    }

    public List<String> generate(String prompt, String systemPrompt, int n) {
        try {
            String requestBody = buildChatRequest(prompt, systemPrompt, 0.7, 2048);
            String response = sendRequest("/chat/completions", requestBody);
            String content = extractChatContent(response);
            return content != null ? List.of(content) : List.of();
        } catch (IOException e) {
            throw new RuntimeException("OpenAI-compatible API error", e);
        }
    }

    public double[] embed(String text) {
        try {
            String requestBody = buildEmbeddingRequest(text);
            String response = sendRequest("/embeddings", requestBody);
            return parseEmbedding(response);
        } catch (IOException e) {
            return deterministicEmbedding(text, embeddingDimension);
        }
    }

    public List<double[]> embedBatch(List<String> texts) {
        try {
            String requestBody = buildBatchEmbeddingRequest(texts);
            String response = sendRequest("/embeddings", requestBody);
            return parseEmbeddingBatch(response);
        } catch (IOException e) {
            return texts.stream()
                .map(t -> deterministicEmbedding(t, embeddingDimension))
                .toList();
        }
    }

    public double classify(String text, String... categories) {
        if (categories.length < 2) {
            throw new IllegalArgumentException("Need at least 2 categories");
        }
        String prompt = "Classify the following text into exactly one of these categories: " +
            String.join(", ", categories) + "\n\n" +
            "Text: " + text + "\n\n" +
            "Category:";
        String result = complete(prompt, 0.1);
        String lower = result.toLowerCase();
        for (String cat : categories) {
            if (lower.contains(cat.toLowerCase())) {
                return 1.0;
            }
        }
        return 0.5;
    }

    public Map<String, Double> predictTokens(String prefix, int topK) {
        return new HashMap<>();
    }

    public boolean isAvailable() {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return false;
            }
            HttpURLConnection conn = (HttpURLConnection)
                URI.create(baseUrl + "/models").toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(2000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public String getModel() {
        return model;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    private String sendRequest(String endpoint, String requestBody) throws IOException {
        URL url = URI.create(baseUrl + endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP " + responseCode + ": " + readError(conn));
        }

        return readResponse(conn);
    }

    private String buildChatRequest(String prompt, String systemPrompt,
                                     double temperature, int maxTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(model).append("\",");
        sb.append("\"messages\":[");

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            sb.append("{\"role\":\"system\",\"content\":\"");
            sb.append(escapeJson(systemPrompt)).append("\"},");
        }

        sb.append("{\"role\":\"user\",\"content\":\"");
        sb.append(escapeJson(prompt)).append("\"}");
        sb.append("],");
        sb.append("\"temperature\":").append(temperature).append(",");
        sb.append("\"max_tokens\":").append(maxTokens);
        sb.append("}");
        return sb.toString();
    }

    private String buildEmbeddingRequest(String text) {
        return String.format(
            "{\"model\":\"%s\",\"input\":\"%s\"}",
            getEmbeddingModel(), escapeJson(text)
        );
    }

    private String buildBatchEmbeddingRequest(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(getEmbeddingModel()).append("\",");
        sb.append("\"input\":");
        sb.append(texts.stream()
            .map(t -> "\"" + escapeJson(t) + "\"")
            .collect(java.util.stream.Collectors.joining(",", "[", "]")));
        sb.append("}");
        return sb.toString();
    }

    private String getEmbeddingModel() {
        if (model.startsWith("gpt-4")) {
            return "text-embedding-3-large";
        } else if (model.startsWith("gpt-3.5")) {
            return "text-embedding-3-small";
        }
        return model;
    }

    private String extractChatContent(String response) {
        int start = response.indexOf("\"content\":\"");
        if (start == -1) {
            return "";
        }
        start += 11;
        int end = findUnescapedQuote(response, start);
        if (end == -1) {
            end = response.indexOf("\"", start);
        }
        if (end == -1) {
            return "";
        }
        return unescapeJson(response.substring(start, end));
    }

    private double[] parseEmbedding(String response) {
        int start = response.indexOf("\"embedding\":[");
        if (start == -1) {
            return new double[0];
        }
        start += 12;
        int end = response.indexOf("]", start);
        if (end == -1) {
            return new double[0];
        }
        String[] values = response.substring(start, end).split(",");
        double[] embedding = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            embedding[i] = Double.parseDouble(values[i].trim());
        }
        return embedding;
    }

    private List<double[]> parseEmbeddingBatch(String response) {
        List<double[]> embeddings = new ArrayList<>();
        String searchKey = "\"embedding\":";
        int idx = response.indexOf(searchKey);
        while (idx != -1) {
            idx += searchKey.length();
            int start = response.indexOf("[", idx);
            if (start == -1) break;
            start++;
            int end = response.indexOf("]", start);
            if (end == -1) break;

            String[] values = response.substring(start, end).split(",");
            double[] embedding = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                embedding[i] = Double.parseDouble(values[i].trim());
            }
            embeddings.add(embedding);

            idx = response.indexOf(searchKey, end);
        }
        return embeddings;
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

    private String readError(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private int findUnescapedQuote(String text, int start) {
        for (int i = start; i < text.length(); i++) {
            if (text.charAt(i) == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    private double[] deterministicEmbedding(String text, int dim) {
        double[] embedding = new double[dim];
        int seed = text.hashCode();
        Random random = new Random(seed);
        for (int i = 0; i < dim; i++) {
            embedding[i] = (random.nextDouble() - 0.5) * 2.0;
        }
        double norm = 0;
        for (double v : embedding) norm += v * v;
        norm = Math.sqrt(norm);
        for (int i = 0; i < dim; i++) embedding[i] /= norm;
        return embedding;
    }
}
