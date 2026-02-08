package com.example.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RagHelper {

    // 환경변수 이름도 헷갈리지 않게 GEMINI_API_KEY로 바꿉니다.
    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
    private static final String PINECONE_API_KEY = System.getenv("PINECONE_API_KEY");
    private static final String PINECONE_HOST = System.getenv("PINECONE_HOST");

    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();

    // 1. Gemini 임베딩 (text-embedding-004 모델 사용)
    public static List<Double> getEmbedding(String text) throws Exception {
        // Gemini는 URL에 API 키를 쿼리 파라미터로 붙입니다.
        String url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key="
                + GEMINI_API_KEY;

        /*
         * 요청 JSON 구조:
         * {
         * "model": "models/text-embedding-004",
         * "content": { "parts": [{ "text": "안녕하세요" }] }
         * }
         */
        JsonObject part = new JsonObject();
        part.addProperty("text", text);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonObject json = new JsonObject();
        json.add("content", content);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini 임베딩 실패: " + response.body());
        }

        // 응답 파싱
        JsonObject resJson = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray values = resJson.getAsJsonObject("embedding").getAsJsonArray("values");

        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            embedding.add(values.get(i).getAsDouble());
        }
        return embedding;
    }

    // 2. Pinecone 저장 (로직 동일, 차원만 768로 바뀜)
    public static void uploadToPinecone(String id, String text, List<Double> vector) throws Exception {
        String url = PINECONE_HOST + "/vectors/upsert";

        JsonObject vectorObj = new JsonObject();
        vectorObj.addProperty("id", id);
        vectorObj.add("values", gson.toJsonTree(vector));

        JsonObject metadata = new JsonObject();
        metadata.addProperty("text", text);
        vectorObj.add("metadata", metadata);

        JsonArray vectors = new JsonArray();
        vectors.add(vectorObj);

        JsonObject payload = new JsonObject();
        payload.add("vectors", vectors);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Api-Key", PINECONE_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Pinecone 저장 실패: " + response.body());
        }
    }

    // 3. Pinecone 검색 (로직 동일)
    public static String searchPinecone(List<Double> vector) throws Exception {
        String url = PINECONE_HOST + "/query";

        JsonObject payload = new JsonObject();
        payload.add("vector", gson.toJsonTree(vector));
        payload.addProperty("topK", 3);
        payload.addProperty("includeMetadata", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Api-Key", PINECONE_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        StringBuilder context = new StringBuilder();
        JsonObject resJson = JsonParser.parseString(response.body()).getAsJsonObject();

        if (resJson.has("matches")) {
            JsonArray matches = resJson.getAsJsonArray("matches");
            for (int i = 0; i < matches.size(); i++) {
                JsonObject match = matches.get(i).getAsJsonObject();
                if (match.has("metadata")) {
                    String text = match.getAsJsonObject("metadata").get("text").getAsString();
                    context.append(text).append("\n---\n");
                }
            }
        }
        return context.toString();
    }

    // 4. [NEW] Gemini에게 질문하기 (generateContent)
    // 기존 Servlet에 있던 callOpenAiApi 대신 이걸 씁니다.
    public static String callGeminiApi(String prompt) throws Exception {
        // gemini-1.5-flash 모델 사용 (빠르고 무료)
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
                + GEMINI_API_KEY;

        /*
         * 요청 JSON 구조:
         * { "contents": [{ "parts": [{ "text": "프롬프트 내용..." }] }] }
         */
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject json = new JsonObject();
        json.add("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini 호출 실패: " + response.body());
        }

        // 응답 파싱
        JsonObject resJson = JsonParser.parseString(response.body()).getAsJsonObject();
        try {
            return resJson.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            return "Gemini 응답 파싱 오류: " + response.body();
        }
    }
}