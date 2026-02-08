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

@WebServlet("/api/todo")
public class TodoApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Gson gson = new Gson();

    // --- JSON 헬퍼 클래스들 ---
    private static class ApiResponse {
        boolean success; String message; Object data; 
        ApiResponse(boolean success, String message, Object data) {
            this.success = success; this.message = message; this.data = data;
        }
    }
    private static class TodoItem {
        int id; String task; boolean is_completed;
    }
    private static class NewTodoRequest {
        String task;
    }
    private static class UpdateTodoRequest {
        int id; boolean is_completed;
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

        List<TodoItem> todoList = new ArrayList<>();
        String sql = "SELECT id, task, is_completed FROM todos WHERE username = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TodoItem item = new TodoItem();
                    item.id = rs.getInt("id");
                    item.task = rs.getString("task");
                    item.is_completed = rs.getBoolean("is_completed");
                    todoList.add(item);
                }
            }
            response.getWriter().write(gson.toJson(new ApiResponse(true, "조회 성공", todoList)));

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
        NewTodoRequest reqData = gson.fromJson(reader, NewTodoRequest.class);
        String task = reqData.task;

        if (task == null || task.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "할 일 내용이 비어있습니다.", null)));
            return;
        }

        String sql = "INSERT INTO todos (username, task) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, username);
            ps.setString(2, task);
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    TodoItem newItem = new TodoItem();
                    newItem.id = generatedKeys.getInt(1);
                    newItem.task = task;
                    newItem.is_completed = false; 
                    response.getWriter().write(gson.toJson(new ApiResponse(true, "추가 성공", newItem)));
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
        UpdateTodoRequest reqData = gson.fromJson(reader, UpdateTodoRequest.class);

        String sql = "UPDATE todos SET is_completed = ? WHERE id = ? AND username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setBoolean(1, reqData.is_completed);
            ps.setInt(2, reqData.id);
            ps.setString(3, username); 
            
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                response.getWriter().write(gson.toJson(new ApiResponse(true, "업데이트 성공", null)));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ApiResponse(false, "항목을 찾을 수 없거나 권한이 없습니다.", null)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 업데이트 오류: " + e.getMessage(), null)));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        
        int id = Integer.parseInt(request.getParameter("id"));

        String sql = "DELETE FROM todos WHERE id = ? AND username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            ps.setString(2, username); 
            
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                response.getWriter().write(gson.toJson(new ApiResponse(true, "삭제 성공", null)));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ApiResponse(false, "항목을 찾을 수 없거나 권한이 없습니다.", null)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ApiResponse(false, "DB 삭제 오류: " + e.getMessage(), null)));
        }
    }
}