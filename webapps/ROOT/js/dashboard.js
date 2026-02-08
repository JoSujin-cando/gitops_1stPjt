// 대시보드가 로드되면 즉시 실행
document.addEventListener('DOMContentLoaded', () => {
    
    // ===================================
    // ======== 기능 1: 메모 ========
    // ===================================
    const memoContent = document.getElementById('memo-content');
    const saveMemoBtn = document.getElementById('save-memo');
    const memoStatus = document.getElementById('memo-status');

    function loadMemo() {
        fetch('api/memo') // MemoApiServlet의 doGet 호출
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    memoContent.value = data.content || ''; 
                } else { alert(data.message); }
            })
            .catch(error => {
                console.error('Memo load error:', error);
                memoStatus.textContent = '메모 로딩 실패.';
            });
    }

    saveMemoBtn.addEventListener('click', () => {
        const content = memoContent.value;
        memoStatus.textContent = '저장 중...';
        
        fetch('api/memo', { // MemoApiServlet의 doPost 호출
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: content }) 
        })
        .then(response => response.json())
        .then(data => {
            memoStatus.textContent = data.success ? '저장 완료!' : ('저장 실패: ' + data.message);
            setTimeout(() => { memoStatus.textContent = ''; }, 2000);
        })
        .catch(error => {
            console.error('Memo save error:', error);
            memoStatus.textContent = '저장 중 오류 발생.';
        });
    });

    // ===================================
    // ======== 기능 2: 투두리스트 ========
    // ===================================
    const todoListEl = document.getElementById('todo-list');
    const newTodoTask = document.getElementById('new-todo-task');
    const addTodoBtn = document.getElementById('add-todo');

    function renderTodos(todos) {
        todoListEl.innerHTML = ''; 
        if (todos.length === 0) {
            const emptyEl = document.createElement('p');
            emptyEl.className = 'empty-list';
            emptyEl.textContent = '할 일이 없습니다. 🌴';
            todoListEl.appendChild(emptyEl);
            return;
        }
        todos.forEach(item => todoListEl.appendChild(createTodoElement(item)));
    }
    
    // (투두 항목 HTML 요소를 생성하는 헬퍼 함수)
    function createTodoElement(item) {
        const todoItemEl = document.createElement('div');
        todoItemEl.className = 'todo-item';
        todoItemEl.dataset.id = item.id; 

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.id = `todo-${item.id}`;
        checkbox.checked = item.is_completed;
        checkbox.addEventListener('change', () => toggleTodoStatus(item.id, checkbox.checked));
        
        const label = document.createElement('label');
        label.setAttribute('for', `todo-${item.id}`);
        label.textContent = item.task;

        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'delete-btn';
        deleteBtn.textContent = 'X';
        deleteBtn.addEventListener('click', () => deleteTodo(item.id, todoItemEl));

        todoItemEl.appendChild(checkbox);
        todoItemEl.appendChild(label);
        todoItemEl.appendChild(deleteBtn);
        return todoItemEl;
    }

    function loadTodos() {
        fetch('api/todo') // TodoApiServlet의 doGet 호출
            .then(response => response.json())
            .then(data => {
                if (data.success) { renderTodos(data.data); } 
                else { alert('투두 로딩 실패: ' + data.message); }
            })
            .catch(error => console.error('Todo load error:', error));
    }

    addTodoBtn.addEventListener('click', () => {
        const task = newTodoTask.value.trim();
        if (task === '') return; 

        fetch('api/todo', { // TodoApiServlet의 doPost 호출
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ task: task })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // (목록의 맨 위에 새 항목 추가)
                if(todoListEl.querySelector('.empty-list')) {
                    todoListEl.innerHTML = '';
                }
                todoListEl.prepend(createTodoElement(data.data));
                newTodoTask.value = ''; // 입력창 비우기
            } else {
                alert('할 일 추가 실패: ' + data.message);
            }
        })
        .catch(error => console.error('Todo add error:', error));
    });
    
    newTodoTask.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') { addTodoBtn.click(); }
    });

    function toggleTodoStatus(id, isCompleted) {
        fetch('api/todo', { // TodoApiServlet의 doPut 호출
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: Number(id), is_completed: isCompleted })
        })
        .then(response => response.json())
        .then(data => {
            if (!data.success) { alert('업데이트 실패: ' + data.message); }
        })
        .catch(error => console.error('Todo update error:', error));
    }

    function deleteTodo(id, elementToRemove) {
        if (!confirm('정말로 이 항목을 삭제하시겠습니까?')) return;

        fetch(`api/todo?id=${id}`, { // TodoApiServlet의 doDelete 호출
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                elementToRemove.remove(); 
                if (todoListEl.children.length === 0) {
                    renderTodos([]);
                }
            } else {
                alert('삭제 실패: ' + data.message);
            }
        })
        .catch(error => console.error('Todo delete error:', error));
    }


    // ========================================================
    // ======== ✨ 기능 3: 필수 암기 리스트 (신규 JS) ========
    // ========================================================
    const memorizeListEl = document.getElementById('memorize-list');

    // (암기 항목 HTML 요소를 생성하는 헬퍼 함수)
    function createMemorizeElement(item) {
        const memorizeItemEl = document.createElement('li');
        memorizeItemEl.className = 'memorize-item';
        memorizeItemEl.dataset.id = item.id;
        
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.id = `memo-item-${item.id}`;
        checkbox.checked = item.is_memorized;
        checkbox.addEventListener('change', () => toggleMemorizeStatus(item.id, checkbox.checked));
        
        const label = document.createElement('label');
        label.setAttribute('for', `memo-item-${item.id}`);
        label.textContent = item.item_text;

        memorizeItemEl.appendChild(checkbox);
        memorizeItemEl.appendChild(label);
        return memorizeItemEl;
    }

    // (페이지 로드 시, 암기 항목 불러오기)
    function loadMemorizeItems() {
        fetch('api/memorize') // MemorizeApiServlet의 doGet 호출
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    memorizeListEl.innerHTML = '';
                    if (data.data.length === 0) {
                        memorizeListEl.innerHTML = '<p class="empty-list">암기 항목이 없습니다.</p>';
                        return;
                    }
                    data.data.forEach(item => memorizeListEl.appendChild(createMemorizeElement(item)));
                } else {
                    alert('암기 항목 로딩 실패: ' + data.message);
                }
            })
            .catch(error => console.error('Memorize load error:', error));
    }

    // (체크박스 클릭 시, 상태 업데이트)
    function toggleMemorizeStatus(itemId, isMemorized) {
        fetch('api/memorize', { // MemorizeApiServlet의 doPut 호출
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ item_id: itemId, is_memorized: isMemorized })
        })
        .then(response => response.json())
        .then(data => {
            if (!data.success) { alert('암기 상태 저장 실패: ' + data.message); }
        })
        .catch(error => console.error('Memorize update error:', error));
    }


    // ========================================================
    // ======== ✨ 기능 4: 유용한 링크 (신규 JS) ========
    // ========================================================
    const linkListEl = document.getElementById('link-list');
    const addLinkForm = document.getElementById('add-link-form');
    const newLinkName = document.getElementById('new-link-name');
    const newLinkUrl = document.getElementById('new-link-url');

    // (링크 항목 HTML 요소를 생성하는 헬퍼 함수)
    function createLinkElement(item) {
        const linkItemEl = document.createElement('li');
        linkItemEl.className = 'link-item';
        linkItemEl.dataset.id = item.id;

        const link = document.createElement('a');
        link.href = item.url.startsWith('http') ? item.url : 'http://' + item.url;
        link.target = '_blank'; // 새 탭에서 열기
        link.textContent = item.link_name;
        
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'delete-btn';
        deleteBtn.textContent = 'X';
        
        if (item.username === null) {
            // 기본 링크(username이 null)는 삭제 버튼 비활성화
            deleteBtn.classList.add('default'); 
        } else {
            // 커스텀 링크만 삭제 이벤트 추가
            deleteBtn.addEventListener('click', () => deleteLink(item.id, linkItemEl));
        }

        linkItemEl.appendChild(link);
        linkItemEl.appendChild(deleteBtn);
        return linkItemEl;
    }

    // (페이지 로드 시, 링크 불러오기)
    function loadLinks() {
        fetch('api/links') // LinkApiServlet의 doGet 호출
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    linkListEl.innerHTML = '';
                    if (data.data.length === 0) {
                        linkListEl.innerHTML = '<p class="empty-list">링크가 없습니다.</p>';
                        return;
                    }
                    data.data.forEach(item => linkListEl.appendChild(createLinkElement(item)));
                } else {
                    alert('링크 로딩 실패: ' + data.message);
                }
            })
            .catch(error => console.error('Links load error:', error));
    }

    // (새 링크 "추가" 버튼 클릭 시)
    addLinkForm.addEventListener('submit', (e) => {
        e.preventDefault(); // 폼 제출(새로고침) 방지
        const name = newLinkName.value.trim();
        const url = newLinkUrl.value.trim();

        if (name === '' || url === '') return;

        fetch('api/links', { // LinkApiServlet의 doPost 호출
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ link_name: name, url: url })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                if(linkListEl.querySelector('.empty-list')) {
                    linkListEl.innerHTML = '';
                }
                linkListEl.appendChild(createLinkElement(data.data)); // 새 링크 추가
                newLinkName.value = '';
                newLinkUrl.value = '';
            } else {
                alert('링크 추가 실패: ' + data.message);
            }
        })
        .catch(error => console.error('Link add error:', error));
    });

    // (링크 삭제 버튼 'X' 클릭 시)
    function deleteLink(id, elementToRemove) {
        if (!confirm('정말로 이 링크를 삭제하시겠습니까?')) return;

        fetch(`api/links?id=${id}`, { // LinkApiServlet의 doDelete 호출
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                elementToRemove.remove();
                if (linkListEl.children.length === 0) {
                    loadLinks([]);
                }
            } else {
                alert('삭제 실패: ' + data.message);
            }
        })
        .catch(error => console.error('Link delete error:', error));
    }


    // --- 페이지 로드 시 최초 실행 ---
    loadMemo();
    loadTodos(); 
    loadMemorizeItems(); // ✨ 암기 항목 로드 함수 호출 추가
    loadLinks();         // ✨ 링크 로드 함수 호출 추가
    
});