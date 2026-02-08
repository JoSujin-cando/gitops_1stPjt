package com.example.servlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    // 1. 환경 변수에서 DB 정보를 읽어옵니다.
    // (이 값들은 Docker 실행 시 -e 옵션으로 주입합니다)
    public static final String DB_URL = System.getenv("DB_URL");
    public static final String DB_USER = System.getenv("DB_USER");
    public static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    /**
     * DB 커넥션을 생성하여 반환합니다.
     * @return Connection 객체
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        // 환경 변수가 설정되지 않았으면 오류를 발생시킵니다.
        if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
            throw new SQLException("데이터베이스 환경 변수(DB_URL, DB_USER, DB_PASSWORD)가 설정되지 않았습니다.");
        }
        
        Class.forName(DB_DRIVER);
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
