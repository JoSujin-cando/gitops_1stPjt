<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String username = (String) session.getAttribute("username");
    if (username == null) {
        response.sendRedirect(request.getContextPath() + "/index.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GPT 질문방 - 수업 도우미</title>
    <link rel="stylesheet" href="css/styles.css"> <link rel="stylesheet" href="css/dashboard.css"> <link rel="stylesheet" href="css/gpt.css"> <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Jua&display=swap">
</head>
<body>
    <header class="dashboard-header">
        <div class="logo">
            <img src="img/robot.png" alt="Robot Logo" class="robot-logo" style="width: 40px; animation: none;">
            <span>RAPA 12기 도우미</span>
        </div>
        <nav>
            <span class="welcome-user"><%= username %>님</span>
            <a href="dashboard.jsp" class="nav-link">대시보드</a>
            <a href="logout" class="nav-link logout-btn">로그아웃</a>
        </nav>
    </header>

    <main class="gpt-container">
        
        <div class="history-list-container">
            <h3>내 질문 목록</h3>
            <ul id="question-list" class="scrollable-list">
                </ul>
        </div>

        <div class="chat-window-container">
            <h3>GPT와 대화하기</h3>
            <div id="chat-history" class="scrollable-list">
                </div>
            
            <div id="gpt-loading" style="display: none;">
                GPT가 열심히 생각 중입니다...
            </div>
            
            <div class="gpt-input-area">
                <textarea id="gpt-prompt" placeholder="질문을 입력하세요... (Shift+Enter로 줄바꿈)"></textarea>
                <button id="ask-gpt" class="btn-submit small">전송</button>
            </div>
        </div>

    </main>

    <script src="js/gpt.js"></script>
</body>
</html>