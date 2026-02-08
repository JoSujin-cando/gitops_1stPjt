document.addEventListener('DOMContentLoaded', () => {
    
    // --- (요소 가져오기) ---
    const chatHistoryEl = document.getElementById('chat-history');
    const gptPromptEl = document.getElementById('gpt-prompt');
    const askGptBtn = document.getElementById('ask-gpt');
    const questionListEl = document.getElementById('question-list');
    const gptLoadingEl = document.getElementById('gpt-loading');
    
    // (로컬에 Q&A 기록 저장)
    let fullHistory = [];

    // --- (헬퍼 함수) ---

    /**
     * (채팅창에 말풍선을 추가하는 함수)
     * @param {string} text - 메시지 내용
     * @param {string} role - 'user' 또는 'bot'
     */
    function renderChatBubble(text, role) {
        const bubble = document.createElement('div');
        bubble.className = `chat-bubble ${role}`;
        bubble.textContent = text;
        chatHistoryEl.appendChild(bubble);
        
        // (가장 아래로 스크롤)
        chatHistoryEl.scrollTop = chatHistoryEl.scrollHeight;
    }

    /**
     * (왼쪽 질문 목록에 항목을 추가하는 함수)
     * @param {object} item - {id, question, answer, ...}
     */
    function renderHistoryListItem(item) {
        const li = document.createElement('li');
        li.className = 'history-item';
        li.dataset.id = item.id;
        li.innerHTML = `<div class="question-preview">${item.question}</div>`;
        
        // (목록 클릭 시, 해당 Q&A를 채팅창에 다시 표시)
        li.addEventListener('click', () => {
            // (모든 항목에서 'active' 클래스 제거)
            questionListEl.querySelectorAll('.history-item').forEach(el => el.classList.remove('active'));
            li.classList.add('active'); // (현재 항목 활성화)

            chatHistoryEl.innerHTML = ''; // (채팅창 비우기)
            renderChatBubble(item.question, 'user');
            renderChatBubble(item.answer, 'bot');
        });
        
        // (새 항목을 목록 맨 위에 추가)
        questionListEl.prepend(li);
        return li;
    }

    /**
     * (새 Q&A 항목을 채팅창과 목록 양쪽에 모두 렌더링)
     * @param {object} item - {id, question, answer, ...}
     * @param {boolean} highlight - 목록에서 활성화할지 여부
     */
    function renderNewHistoryEntry(item, highlight = false) {
        const li = renderHistoryListItem(item);
        if (highlight) {
            li.click(); // (방금 질문한 항목을 바로 클릭)
        }
    }


    // --- (API 통신 함수) ---

    /**
     * (1. 페이지 로드 시)
     * GptApiServlet(GET)을 호출하여 모든 Q&A 기록을 불러옵니다.
     */
    function loadHistory() {
        gptLoadingEl.style.display = 'block';
        gptLoadingEl.textContent = '과거 기록을 불러오는 중...';

        fetch('api/gpt') // GptApiServlet.doGet 호출
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    fullHistory = data.data; // (전체 기록 저장)
                    questionListEl.innerHTML = ''; // (목록 비우기)
                    chatHistoryEl.innerHTML = ''; // (채팅창 비우기)

                    if (fullHistory.length > 0) {
                        // (모든 기록을 목록과 채팅창에 렌더링)
                        fullHistory.forEach(item => {
                            renderHistoryListItem(item);
                            renderChatBubble(item.question, 'user');
                            renderChatBubble(item.answer, 'bot');
                        });
                        // (가장 마지막 항목을 활성화)
                        questionListEl.querySelector('.history-item').classList.add('active');
                    } else {
                        questionListEl.innerHTML = '<p class="empty-list">질문 기록이 없습니다.</p>';
                        renderChatBubble('안녕하세요! RAPA AWS 12기 수업에 대해 무엇이든 물어보세요.', 'bot');
                    }
                } else {
                    alert('기록 로딩 실패: ' + data.message);
                }
            })
            .catch(error => console.error('History load error:', error))
            .finally(() => gptLoadingEl.style.display = 'none');
    }

    /**
     * (2. '전송' 버튼 클릭 시)
     * GptApiServlet(POST)을 호출하여 새 질문을 보냅니다.
     */
    function askQuestion() {
        const prompt = gptPromptEl.value.trim();
        if (prompt === '') return;
        
        // (기존 채팅창에 내 질문 먼저 표시)
        renderChatBubble(prompt, 'user');
        gptPromptEl.value = ''; // (입력창 비우기)
        gptLoadingEl.style.display = 'block'; // (로딩 시작)
        askGptBtn.disabled = true;

        fetch('api/gpt', { // GptApiServlet.doPost 호출
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: prompt })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // (서버가 돌려준 새 Q&A 객체(data.data)를 렌더링)
                renderNewHistoryEntry(data.data, true); 
            } else {
                alert('GPT 답변 실패: ' + data.message);
                renderChatBubble('오류가 발생했습니다: ' + data.message, 'bot');
            }
        })
        .catch(error => {
            console.error('GPT ask error:', error);
            renderChatBubble('서버와 통신 중 심각한 오류가 발생했습니다.', 'bot');
        })
        .finally(() => {
            gptLoadingEl.style.display = 'none'; // (로딩 끝)
            askGptBtn.disabled = false;
        });
    }

    // --- (이벤트 리스너 연결) ---
    askGptBtn.addEventListener('click', askQuestion);
    
    // (Shift+Enter는 줄바꿈, Enter는 전송)
    gptPromptEl.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault(); // (textarea의 기본 Enter 동작 방지)
            askGptBtn.click();
        }
    });

    // (페이지 로드 시 최초 1회 기록 불러오기)
    loadHistory();
});