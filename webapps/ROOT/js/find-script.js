document.addEventListener('DOMContentLoaded', function() {
    // 탭 전환 기능
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    const tabSlider = document.querySelector('.tab-slider-indicator');
    
    // 초기 탭 슬라이더 위치 설정
    if (tabSlider) {
        const activeTab = document.querySelector('.tab-btn.active');
        const activeIndex = Array.from(tabBtns).indexOf(activeTab);
        updateTabSlider(activeIndex);
    }
    
    function updateTabSlider(activeIndex) {
        // 탭이 2개인 경우 0 또는 1의 인덱스를 가짐
        // 0일 때는 0%, 1일 때는 50%로 이동
        const position = activeIndex * 100; // 50%
        tabSlider.style.transform = `translateX(${position}%)`;
    }
    
    tabBtns.forEach((btn, index) => {
        btn.addEventListener('click', function() {
            // 탭 버튼 활성화
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            // 탭 슬라이더 이동 - 반드시 인덱스로 이동
            updateTabSlider(index);
            
            // 탭 컨텐츠 활성화 - 애니메이션 효과 추가
            const tabId = this.getAttribute('data-tab');
            
            // 현재 활성화된 탭을 비활성화
            tabContents.forEach(content => {
                if (content.classList.contains('active')) {
                    // 페이드아웃 애니메이션
                    content.style.opacity = '0';
                    content.style.transform = 'translateY(10px)';
                    
                    setTimeout(() => {
                        content.classList.remove('active');
                        
                        // 새 탭을 활성화
                        const newContent = document.getElementById(tabId);
                        newContent.classList.add('active');
                        
                        // 페이드인 애니메이션 적용을 위해 약간의 지연 추가
                        setTimeout(() => {
                            newContent.style.opacity = '1';
                            newContent.style.transform = 'translateY(0)';
                        }, 50);
                    }, 200);
                }
            });
            
            // 메시지 초기화
            document.querySelectorAll('.message').forEach(msg => {
                msg.textContent = '';
                msg.className = 'message';
            });
        });
    });

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
    
    // 아이디 찾기 폼 제출
    const findIdForm = document.getElementById('findIdForm');
    const findIdMessage = document.getElementById('find-id-message');
    
    if (findIdForm) {
        findIdForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const email = document.getElementById('find-id-email').value;
            const name = document.getElementById('find-id-name').value;
            const submitButton = this.querySelector('button[type="submit"]');
            
            // 로딩 상태 표시
            submitButton.disabled = true;
            submitButton.innerHTML = '찾는 중 <span class="loading"></span>';
            findIdMessage.textContent = '';
            
            // API 요청 시뮬레이션
            setTimeout(() => {
                // 임시 데이터 (실제로는 서버에서 확인)
                if (email === 'test@example.com' && name === '홍길동') {
                    findIdMessage.innerHTML = '회원님의 아이디는 <strong>testuser</strong> 입니다.';
                    findIdMessage.className = 'message success';
                } else {
                    findIdMessage.textContent = '일치하는 정보가 없습니다.';
                    findIdMessage.className = 'message error';
                }
                
                submitButton.disabled = false;
                submitButton.textContent = '아이디 찾기';
            }, 1000); // 서버 응답 시뮬레이션
        });
    }
    
    // 비밀번호 찾기 폼 제출
    const findPwForm = document.getElementById('findPwForm');
    const findPwMessage = document.getElementById('find-pw-message');
    
    if (findPwForm) {
        findPwForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const email = document.getElementById('find-pw-email').value;
            const name = document.getElementById('find-pw-name').value;
            const birthdate = document.getElementById('find-pw-birthdate').value;
            const submitButton = this.querySelector('button[type="submit"]');
            
            // 생년월일 유효성 검사
            if (birthdate.length !== 8 || !/^\d+$/.test(birthdate)) {
                findPwMessage.textContent = '생년월일은 8자리 숫자로 입력해주세요.';
                findPwMessage.className = 'message error';
                return;
            }
            
            // 로딩 상태 표시
            submitButton.disabled = true;
            submitButton.innerHTML = '찾는 중 <span class="loading"></span>';
            findPwMessage.textContent = '';
            
            // API 요청 시뮬레이션
            setTimeout(() => {
                // 임시 데이터 (실제로는 서버에서 확인)
                if (email === 'test@example.com' && name === '홍길동' && birthdate === '19900101') {
                    findPwMessage.innerHTML = '회원님의 비밀번호는 <strong>password123</strong> 입니다.';
                    findPwMessage.className = 'message success';
                } else {
                    findPwMessage.textContent = '일치하는 정보가 없습니다.';
                    findPwMessage.className = 'message error';
                }
                
                submitButton.disabled = false;
                submitButton.textContent = '비밀번호 찾기';
            }, 1000); // 서버 응답 시뮬레이션
        });
    }
});