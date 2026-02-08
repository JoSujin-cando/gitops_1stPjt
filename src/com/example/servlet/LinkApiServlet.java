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

@WebServlet("/api/links")
public class LinkApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Gson gson = new Gson();

    // --- JSON 헬퍼 클래스들 ---
    private static class ApiResponse {
        boolean success; String message; Object data; 
        ApiResponse(boolean success, String message, Object data) {
            this.success = success; this.message = message; this.data = data;
        }
    }
    private static class LinkItem {
        int id; String username; String link_name; String url;
    }
    private static class NewLinkRequest {
        String link_name; String url;
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

        List<LinkItem> linkList = new ArrayList<>();
        String sql = "SELECT id, username, link_name, url FROM links WHERE username IS NULL OR username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LinkItem item = new LinkItem();
                    item.id = rs.getInt("id");
                    item.username = rs.getString("username");
                    item.link_name = rs.getString("link_name");
                    item.url = rs.getString("url");
                    linkList.add(item);
                }
            }
            response.getWriter().write(gson.toJson(new ApiResponse(true, "조회 성공", linkList)));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 조회 오류: " + e.getMessage(), null)));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        NewLinkRequest reqData = gson.fromJson(reader, NewLinkRequest.class);

        if (reqData.link_name == null || reqData.link_name.trim().isEmpty() ||
            reqData.url == null || reqData.url.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "링크 이름과 URL을 모두 입력해야 합니다.", null)));
            return;
        }

        String sql = "INSERT INTO links (username, link_name, url) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, username);
            ps.setString(2, reqData.link_name);
            ps.setString(3, reqData.url);
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    LinkItem newItem = new LinkItem();
                    newItem.id = generatedKeys.getInt(1);
                    newItem.username = username;
                    newItem.link_name = reqData.link_name;
                    newItem.url = reqData.url;
                    response.getWriter().write(gson.toJson(new ApiResponse(true, "링크 추가 성공", newItem)));
                } else {
                    throw new SQLException("ID 생성 실패");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 저장 오류: " + e.getMessage(), null)));
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "로그인이 필요합니다.", null)));
            return;
        }
        String username = (String) session.getAttribute("username");
        
        int id = Integer.parseInt(request.getParameter("id"));

        String sql = "DELETE FROM links WHERE id = ? AND username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            ps.setString(2, username);
            
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                response.getWriter().write(gson.toJson(new ApiResponse(true, "삭제 성공", null)));
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(gson.toJson(new ApiResponse(false, "기본 링크는 삭제할 수 없거나 존재하지 않는 링크입니다.", null)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 삭제 오류: " + e.getMessage(), null)));
        }
    }
}