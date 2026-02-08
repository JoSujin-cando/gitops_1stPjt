<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // 세션에서 사용자 이름 가져오기
    String username = (String) session.getAttribute("username");
    
    // 세션이 없으면(로그인 안 했으면) 로그인 페이지로 돌려보냄
    if (username == null) {
        response.sendRedirect(request.getContextPath() + "/index.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>환영합니다!</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/styles.css" />
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Jua&display=swap">
</head>
<body style="padding-top: 50px;">
  <div class="container" style="max-width: 500px;">
    <div class="auth-card">
        <div class="logo">
             <img src="<%=request.getContextPath()%>/img/robot.png" alt="Robot Logo" class="robot-logo">
        </div>
        <h2 style="text-align: center; font-family: 'Jua', sans-serif; margin-bottom: 20px;">
            <%= username %>님, 환영합니다!
        </h2>
        <p style="text-align: center; margin-bottom: 30px;">
            RAPA AWS 12기 수업 도우미에 오신 것을 환영합니다.
        </p>
        
        <a href="dashboard.jsp" class="btn-submit" style="text-decoration: none; text-align: center; line-height: 46px; margin-bottom: 15px;">
            수업 도우미 접속하기
        </a>
        
        <a href="logout" class="text-link" style="display: block; text-align: center;">
            로그아웃
        </a>
    </div>
  </div>
</body>
</html>