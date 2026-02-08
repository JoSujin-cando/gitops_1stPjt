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
    <title>RAPA AWS 12기 수업 도우미</title>
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/dashboard.css"> 
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Jua&display=swap">
</head>
<body>
    <header class="dashboard-header">
        <div class="logo">
            <img src="img/robot.png" alt="Robot Logo" class="robot-logo" style="width: 40px; animation: none;">
            <span>RAPA 12기 도우미</span>
        </div>
        <nav>
            <span class="welcome-user"><%= username %>님</span>
            <a href="gpt.jsp" class="nav-link">GPT 질문방</a>
            <a href="logout" class="nav-link logout-btn">로그아웃</a>
        </nav>
    </header>

    <main class="dashboard-grid">
        
        <section class="widget memo-widget">
            <h3>📝 각자의 메모</h3>
            <textarea id="memo-content" placeholder="여기에 메모를 입력하세요..."></textarea>
            <button id="save-memo" class="btn-submit small">저장</button>
            <span id="memo-status" class="status-msg"></span>
        </section>
        
        <section class="widget todo-widget">
            <h3>✅ 투두리스트</h3>
            <div id="todo-list" class="scrollable-list"></div>
            <div class="todo-input-group">
                <input type="text" id="new-todo-task" placeholder="새 할 일...">
                <button id="add-todo" class="btn-submit small">+</button>
            </div>
        </section>

        <section class="widget memorize-widget">
            <h3>🧠 필수 암기 리스트</h3>
            <ul id="memorize-list" class="scrollable-list">
                </ul>
        </section>
        
        <section class="widget links-widget">
            <h3>🔗 유용한 링크</h3>
            <ul id="link-list" class="scrollable-list">
                 </ul>
            <form id="add-link-form" class="add-link-form">
                <input type="text" id="new-link-name" placeholder="링크 이름" required>
                <input type="text" id="new-link-url" placeholder="URL (http://...)" required>
                <button type="submit" class="btn-submit small">추가</button>
            </form>
        </section>

    </main>
    <script src="js/dashboard.js"></script>
</body>
</html>