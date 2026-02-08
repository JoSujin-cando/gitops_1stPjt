package com.example.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/signup")
public class SignupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("index.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String name = request.getParameter("name");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String birthdate = request.getParameter("birthdate");

        // 필수값 체크
        if (username == null || username.isEmpty()
                || password == null || password.isEmpty()
                || email == null || email.isEmpty()) {
            response.getWriter().write("{\"success\": false, \"message\": \"필수 입력값이 누락되었습니다.\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            // ✅ 아이디 / 이메일 중복 체크 (쿼리 1번)
            String checkSql = "SELECT username, email FROM users WHERE username = ? OR email = ?";
            boolean usernameExists = false;
            boolean emailExists = false;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                checkStmt.setString(2, email);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    while (rs.next()) {
                        String existingUsername = rs.getString("username");
                        String existingEmail = rs.getString("email");

                        if (existingUsername != null && existingUsername.equals(username)) {
                            usernameExists = true;
                        }
                        if (existingEmail != null && existingEmail.equals(email)) {
                            emailExists = true;
                        }
                    }
                }
            }

            if (usernameExists) {
                conn.rollback();
                response.getWriter().write("{\"success\": false, \"message\": \"이미 존재하는 아이디입니다.\"}");
                return;
            }

            if (emailExists) {
                conn.rollback();
                response.getWriter().write("{\"success\": false, \"message\": \"이미 존재하는 이메일입니다.\"}");
                return;
            }

            // 🔽 여기부터는 실제 INSERT 로직 (너 기존 코드에 맞게 수정해도 됨)
            String insertSql =
                    "INSERT INTO users (email, name, username, password, birthdate) VALUES (?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(insertSql);
            ps.setString(1, email);
            ps.setString(2, name);
            ps.setString(3, username);
            ps.setString(4, password);   // 필요하면 여기서 해시 적용
            ps.setString(5, birthdate);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                conn.commit();
                response.getWriter().write("{\"success\": true, \"message\": \"회원가입이 완료되었습니다.\"}");
            } else {
                conn.rollback();
                response.getWriter().write("{\"success\": false, \"message\": \"회원가입에 실패했습니다.\"}");
            }

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException se) { se.printStackTrace(); }
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            if (e.getMessage() != null && e.getMessage().contains("환경 변수")) {
                response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"DB 작업 중 오류가 발생했습니다.\"}");
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"message\": \"DB 드라이버를 찾을 수 없습니다.\"}");

        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException ignore) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignore) {}
        }
    }
}

