document.addEventListener('DOMContentLoaded', function() {
    // 탭 전환 기능 (원본과 동일)
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    const tabSlider = document.querySelector('.tab-slider-indicator');
    
    if (tabSlider) {
        const activeTab = document.querySelector('.tab-btn.active');
        const activeIndex = Array.from(tabBtns).indexOf(activeTab);
        updateTabSlider(activeIndex);
    }
    
    function updateTabSlider(activeIndex) {
        const position = activeIndex * 100; // 탭이 2개이므로 0% 또는 50%
        tabSlider.style.transform = `translateX(${position}%)`;
    }
    
    tabBtns.forEach((btn, index) => {
        btn.addEventListener('click', function() {
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            // 탭 슬라이더 이동 (인덱스가 아닌 data-tab 속성값으로 위치 계산)
            // 0 -> 0%, 1 -> 50%
            updateTabSlider(index);
            
            const tabId = this.getAttribute('data-tab');
            tabContents.forEach(content => {
                content.classList.remove('active');
            });
            document.getElementById(tabId).classList.add('active');
            
            document.querySelectorAll('.message').forEach(msg => {
                msg.textContent = '';
                msg.className = 'message';
            });
        });
    });

    // ---------------------------------------------------
    // ✨ [수정됨] 로그인 폼 제출 (AJAX Fetch)
    // ---------------------------------------------------
    const loginForm = document.getElementById('loginForm');
    const loginMessage = document.getElementById('login-message');
    
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const submitButton = this.querySelector('button[type="submit"]');
            const formData = new URLSearchParams(new FormData(this));

            // 로딩 상태
            submitButton.disabled = true;
            submitButton.innerHTML = '로그인 중 <span class="loading"></span>';
            loginMessage.textContent = '';
            loginMessage.className = 'message';

            // /login 서블릿에 POST 요청
            fetch('login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    loginMessage.textContent = '로그인 성공!';
                    loginMessage.className = 'message success';
                    // 성공 시 welcome 페이지로 이동
                    window.location.href = data.redirect;
                } else {
                    loginMessage.textContent = data.message || '로그인 실패';
                    loginMessage.className = 'message error';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                loginMessage.textContent = '서버 통신 중 오류가 발생했습니다.';
                loginMessage.className = 'message error';
            })
            .finally(() => {
                submitButton.disabled = false;
                submitButton.textContent = '로그인';
            });
        });
    }

    // ---------------------------------------------------
    // ✨ [수정됨] 회원가입 폼 제출 (AJAX Fetch)
    // ---------------------------------------------------
    const registerForm = document.getElementById('registerForm');
    const registerMessage = document.getElementById('register-message');
    
    if (registerForm) {
        // (아이디 중복 체크 기능은 편의상 제거 - 서블릿에서 최종 체크)
        
        registerForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const password = document.getElementById('register-password').value;
            const birthdate = document.getElementById('register-birthdate').value;
            const submitButton = this.querySelector('button[type="submit"]');

            if (password.length < 6) {
                registerMessage.textContent = '비밀번호는 6자 이상이어야 합니다.';
                registerMessage.className = 'message error';
                return;
            }
            if (birthdate.length !== 8 || !/^\d+$/.test(birthdate)) {
                registerMessage.textContent = '생년월일은 8자리 숫자로 입력해주세요 (예: 19900101).';
                registerMessage.className = 'message error';
                return;
            }

            const formData = new URLSearchParams(new FormData(this));

            // 로딩 상태
            submitButton.disabled = true;
            submitButton.innerHTML = '처리 중 <span class="loading"></span>';
            registerMessage.textContent = '';
            registerMessage.className = 'message';

            // /signup 서블릿에 POST 요청
            fetch('signup', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    registerMessage.textContent = data.message || '회원가입이 완료되었습니다.';
                    registerMessage.className = 'message success';
                    registerForm.reset();
                    // 2초 후 로그인 탭으로 자동 전환
                    setTimeout(() => {
                        document.querySelector('.tab-btn[data-tab="login"]').click();
                    }, 2000);
                } else {
                    registerMessage.textContent = data.message || '회원가입 실패';
                    registerMessage.className = 'message error';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                registerMessage.textContent = '서버 통신 중 오류가 발생했습니다.';
                registerMessage.className = 'message error';
            })
            .finally(() => {
                submitButton.disabled = false;
                submitButton.textContent = '회원가입';
            });
        });
    }
    
    // 탭 슬라이더 버그 수정 (index.html 원본 코드 기반)
    tabBtns.forEach((btn, index) => {
        btn.addEventListener('click', function() {
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            // 탭 슬라이더 이동 (index 기반)
            tabSlider.style.transform = `translateX(${index * 100}%)`; // 0 또는 50%
            
            const tabId = this.getAttribute('data-tab');
            tabContents.forEach(content => {
                content.classList.remove('active');
            });
            document.getElementById(tabId).classList.add('active');
        });
    });

    // 초기 탭 활성화 (수정)
    const initialActiveTab = document.querySelector('.tab-btn.active');
    if (initialActiveTab) {
        const initialTabId = initialActiveTab.getAttribute('data-tab');
        document.getElementById(initialTabId).classList.add('active');
    }
});