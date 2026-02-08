<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>회원가입</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/signup.css" />
</head>
<body>
  <div class="signup-container">
    <h1>회원가입</h1>
    <form action="${pageContext.request.contextPath}/signup" method="POST">
      <input type="text" name="username" placeholder="아이디" required />
      <input type="password" name="password" placeholder="비밀번호" required />
      <input type="password" name="passwordConfirm" placeholder="비밀번호 확인" required />
      <input type="email" name="email" placeholder="이메일" required />
      <button type="submit">가입하기</button>
    </form>
    <div class="login-link">
   
   이미 회원이신가요? <a href="${pageContext.request.contextPath}/index.html">로그인</a>
    </div>
    <%-- 에러 메시지 출력 예시 --%>
    <c:if test="${not empty errorMessage}">
      <p style="color:red">${errorMessage}</p>
    </c:if>
  </div>
</body>
</html>