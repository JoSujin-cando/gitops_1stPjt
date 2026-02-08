package com.example.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import com.google.gson.Gson; 

@WebServlet("/api/memorize")
public class MemorizeApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Gson gson = new Gson();

    // --- JSON 헬퍼 클래스들 ---
    private static class ApiResponse {
        boolean success; String message; Object data; 
        ApiResponse(boolean success, String message, Object data) {
            this.success = success; this.message = message; this.data = data;
        }
    }
    private static class MemorizeItem {
        int id; String item_text; boolean is_memorized;
    }
    private static class UpdateMemorizeRequest {
        int item_id; boolean is_memorized;
    }
    // --- 헬퍼 클래스 끝 ---

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "로그인이 필요합니다.", null)));
            return;
        }
        String username = (String) session.getAttribute("username");

        List<MemorizeItem> memorizeList = new ArrayList<>();
        String sql = "SELECT m.id, m.item_text, IFNULL(s.is_memorized, 0) as is_memorized " +
                     "FROM memorize_items m " +
                     "LEFT JOIN user_memorize_status s ON m.id = s.item_id AND s.username = ? " +
                     "ORDER BY m.sort_order, m.id";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MemorizeItem item = new MemorizeItem();
                    item.id = rs.getInt("id");
                    item.item_text = rs.getString("item_text");
                    item.is_memorized = rs.getBoolean("is_memorized");
                    memorizeList.add(item);
                }
            }
            response.getWriter().write(gson.toJson(new ApiResponse(true, "조회 성공", memorizeList)));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 조회 오류: " + e.getMessage(), null)));
        }
    }

    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        UpdateMemorizeRequest reqData = gson.fromJson(reader, UpdateMemorizeRequest.class);

        String sql = "INSERT INTO user_memorize_status (username, item_id, is_memorized) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE is_memorized = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            ps.setInt(2, reqData.item_id);
            ps.setBoolean(3, reqData.is_memorized);
            ps.setBoolean(4, reqData.is_memorized); // UPDATE용
            
            ps.executeUpdate();
            
            response.getWriter().write(gson.toJson(new ApiResponse(true, "상태 저장 성공", null)));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 저장 오류: " + e.getMessage(), null)));
        }
    }
}