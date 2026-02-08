package com.example.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.io.BufferedReader;
import com.google.gson.Gson;
import com.example.util.RagHelper;
import java.util.List;

@WebServlet("/api/memo")
public class MemoApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Gson gson = new Gson();

    private static class ApiResponse {
        boolean success;
        String message;
        String content;

        ApiResponse(boolean success, String message, String content) {
            this.success = success;
            this.message = message;
            this.content = content;
        }
    }

    private static class MemoSaveRequest {
        String content;
    }

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

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT content FROM memos WHERE username = ?")) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String content = rs.getString("content");
                    response.getWriter().write(gson.toJson(new ApiResponse(true, "조회 성공", content)));
                } else {
                    response.getWriter().write(gson.toJson(new ApiResponse(true, "새 메모", "")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 조회 오류: " + e.getMessage(), null)));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // --- [여기서부터는 기존 코드와 동일] ---
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

        BufferedReader reader = request.getReader();
        MemoSaveRequest reqData = gson.fromJson(reader, MemoSaveRequest.class);
        String newContent = reqData.content;

        // DB 저장 로직
        String sql = "INSERT INTO memos (username, content) VALUES (?, ?) ON DUPLICATE KEY UPDATE content = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, newContent);
            ps.setString(3, newContent);

            ps.executeUpdate();

            // --- [기존 코드 끝 / 추가된 부분 시작] ---

            // [RAG 추가] MySQL 저장이 성공하면, Pinecone(벡터DB)에도 저장함
            // 그래야 나중에 GPT가 이 내용을 검색할 수 있음
            try {
                if (newContent != null && !newContent.trim().isEmpty()) {
                    System.out.println("[RAG] Pinecone 동기화 시작...");

                    // 1. 텍스트를 숫자로 변환
                    List<Double> vector = RagHelper.getEmbedding(newContent);

                    // 2. Pinecone에 업로드
                    // (ID는 "memo_유저명"으로 해서, 유저당 하나의 메모패드만 계속 업데이트되게 함)
                    RagHelper.uploadToPinecone("memo_" + username, newContent, vector);

                    System.out.println("[RAG] Pinecone 동기화 완료");
                }
            } catch (Exception e) {
                // 여기가 중요함: Pinecone 저장이 실패하더라도, 사용자의 메모 저장은 성공한 걸로 쳐야 함.
                // 그래서 에러를 잡아서 로그만 찍고 넘어감 (사용자에게 에러 팝업 안 띄움)
                System.err.println("!! 주의 !! Pinecone 동기화 실패: " + e.getMessage());
                e.printStackTrace();
            }

            // --- [추가된 부분 끝] ---

            response.getWriter().write(gson.toJson(new ApiResponse(true, "저장되었습니다.", null)));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 저장 오류: " + e.getMessage(), null)));
        }
    }
}