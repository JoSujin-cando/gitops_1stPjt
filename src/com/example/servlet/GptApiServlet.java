package com.example.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.example.util.RagHelper;

@WebServlet("/api/gpt")
public class GptApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // API 키를 환경 변수에서 읽어옵니다.
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");

    private final Gson gson = new Gson();

    // --- JSON 헬퍼 클래스들 ---
    private static class ApiResponse {
        boolean success;
        String message;
        Object data;

        ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }

    private static class GptHistoryItem {
        int id;
        String question;
        String answer;
        String created_at;
    }

    private static class GptPromptRequest {
        String prompt;
    }

    private static class GptResponse {
        GptChoice[] choices;
    }

    private static class GptChoice {
        GptMessage message;
    }

    private static class GptMessage {
        String role;
        String content;
    }

    // OpenAI API 요청용 클래스 (JSON 주입 방지)
    private static class GptRequest {
        String model;
        List<GptRequestMessage> messages;

        GptRequest(String model, String prompt) {
            this.model = model;
            this.messages = new ArrayList<>();
            this.messages.add(new GptRequestMessage("user", prompt));
        }
    }

    private static class GptRequestMessage {
        String role;
        String content;

        GptRequestMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    // --- 헬퍼 클래스 끝 ---

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "로그인이 필요합니다.", null)));
            return;
        }
        String username = (String) session.getAttribute("username");

        List<GptHistoryItem> historyList = new ArrayList<>();
        String sql = "SELECT id, question, answer, created_at FROM gpt_history WHERE username = ? ORDER BY created_at ASC";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GptHistoryItem item = new GptHistoryItem();
                    item.id = rs.getInt("id");
                    item.question = rs.getString("question");
                    item.answer = rs.getString("answer");
                    item.created_at = rs.getTimestamp("created_at").toString();
                    historyList.add(item);
                }
            }
            response.getWriter().write(gson.toJson(new ApiResponse(true, "조회 성공", historyList)));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 조회 오류: " + e.getMessage(), null)));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. 기본 설정 및 세션 체크
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "로그인이 필요합니다.", null)));
            return;
        }
        String username = (String) session.getAttribute("username");

        // 2. 요청 파싱 (여기서 prompt 변수가 만들어집니다!)
        BufferedReader reader = request.getReader();
        GptPromptRequest reqData = gson.fromJson(reader, GptPromptRequest.class);

        // [핵심] 에러가 났던 이유: 이 줄이 없어서였습니다.
        String prompt = reqData.prompt;

        // 유효성 검사
        if (prompt == null || prompt.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "질문 내용이 비어있습니다.", null)));
            return;
        }

        try {
            // 3. [RAG] Pinecone 검색 (Gemini 임베딩 사용)
            String relatedContext = "";
            try {
                // RagHelper가 내부적으로 Gemini API를 써서 벡터를 만듭니다.
                List<Double> vector = RagHelper.getEmbedding(prompt);
                relatedContext = RagHelper.searchPinecone(vector);
                System.out.println("[RAG 검색 결과] " + relatedContext);
            } catch (Exception e) {
                System.err.println("[RAG 오류] 검색 실패 (답변은 계속 진행): " + e.getMessage());
            }

            // 4. 프롬프트 구성 (검색된 지식 + 원래 질문)
            String finalPrompt;
            if (relatedContext != null && !relatedContext.isEmpty()) {
                finalPrompt = "당신은 IT 학습 도우미입니다. 아래 [학습 메모]를 참고하여 질문에 답해주세요.\n" +
                        "메모에 없는 내용은 당신의 일반적인 지식으로 답변하세요.\n\n" +
                        "[학습 메모]\n" + relatedContext + "\n\n" +
                        "[질문]\n" + prompt;
            } else {
                finalPrompt = prompt;
            }

            // 5. [Gemini 호출] (기존 callOpenAiApi 대신 RagHelper 사용)
            String answer = RagHelper.callGeminiApi(finalPrompt);

            // 6. DB 저장 (질문 내역 기록)
            // 주의: DB에는 '검색된 내용이 섞인 finalPrompt'가 아니라 사용자의 '원래 질문(prompt)'을 저장합니다.
            String sql = "INSERT INTO gpt_history (username, question, answer) VALUES (?, ?, ?)";
            GptHistoryItem newHistoryItem = new GptHistoryItem();

            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, username);
                ps.setString(2, prompt); // 사용자가 입력한 질문 저장
                ps.setString(3, answer);
                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newHistoryItem.id = generatedKeys.getInt(1);
                        newHistoryItem.question = prompt;
                        newHistoryItem.answer = answer;
                        newHistoryItem.created_at = new Timestamp(System.currentTimeMillis()).toString();
                    } else {
                        throw new SQLException("ID 생성 실패");
                    }
                }
            }

            // 7. 결과 응답
            response.getWriter().write(gson.toJson(new ApiResponse(true, "질문 성공", newHistoryItem)));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "오류 발생: " + e.getMessage(), null)));
        }
    }
}
