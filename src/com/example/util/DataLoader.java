package com.example.util; // RagHelper와 같은 방(패키지)이라고 선언

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Base64; // [추가됨] 암호화 도구
import java.nio.charset.StandardCharsets; // [추가됨] 문자셋 도구

public class DataLoader {
    public static void main(String[] args) {
        // ---------------------------------------------------------------
        // [수정 필요] .md 파일들이 들어있는 내 컴퓨터 경로 (역슬래시 2개씩 써야 함)
        String folderPath = "C:\\Users\\DS10\\Downloads\\AWS Cloud";
        // ---------------------------------------------------------------

        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            System.out.println("폴더가 비어있거나 경로가 잘못되었습니다: " + folderPath);
            return;
        }

        System.out.println("데이터 적재를 시작합니다...");

        int count = 0;
        for (File file : listOfFiles) {
            // .md 파일만 골라서 처리
            if (file.isFile() && file.getName().endsWith(".md")) {
                try {
                    System.out.print("처리 중: " + file.getName() + " ... ");

                    // 1. 파일 내용 읽기 (Java 11 이상 기능)
                    String content = Files.readString(file.toPath());

                    // 2. 벡터 변환 (RagHelper 사용)
                    List<Double> vector = RagHelper.getEmbedding(content);

                    String safeId = Base64.getEncoder().encodeToString(file.getName().getBytes(StandardCharsets.UTF_8));

                    // 3. Pinecone 업로드 (ID는 파일명, 내용은 Metadata로 저장)
                    RagHelper.uploadToPinecone("file_" + safeId, content, vector);

                    count++;
                    System.out.println("[성공]");

                    // API 속도 제한 방지를 위해 0.5초 대기
                    Thread.sleep(500);

                } catch (Exception e) {
                    System.out.println("[실패] 이유: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("------------------------------------------------");
        System.out.println("총 " + count + "개의 파일이 Pinecone에 저장되었습니다.");
    }
}