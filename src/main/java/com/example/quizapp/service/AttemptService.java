package com.example.quizapp.service;

import com.example.quizapp.model.Attempt;
import java.util.List;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class AttemptService {

    private final DataSource dataSource;

    public AttemptService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // saves a new attempt to the storage system
    public void saveAttempt(Attempt a) {
        String attemptSql = "INSERT INTO attempts (username, quiz_id, score, total, timestamp) VALUES (?, ?, ?, ?, ?)";
        String answersSql = "INSERT INTO attempt_answers (attempt_id, question_id, submitted_answer) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            int attemptId = -1;
            try (PreparedStatement attemptStmt = conn.prepareStatement(attemptSql, Statement.RETURN_GENERATED_KEYS)) {
                attemptStmt.setString(1, a.getUsername());
                attemptStmt.setInt(2, a.getQuizId());
                attemptStmt.setInt(3, a.getScore());
                attemptStmt.setInt(4, a.getTotal());
                attemptStmt.setString(5, a.getTimestamp());
                attemptStmt.executeUpdate();
                
                try (ResultSet rs = attemptStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        attemptId = rs.getInt(1);
                        a.setId(attemptId);
                    }
                }
            }
            
            if (attemptId != -1 && a.getAnswers() != null && !a.getAnswers().isEmpty()) {
                try (PreparedStatement ansStmt = conn.prepareStatement(answersSql)) {
                    for (Map.Entry<Integer, Integer> entry : a.getAnswers().entrySet()) {
                        ansStmt.setInt(1, attemptId);
                        ansStmt.setInt(2, entry.getKey());
                        ansStmt.setInt(3, entry.getValue());
                        ansStmt.addBatch();
                    }
                    ansStmt.executeBatch();
                }
            }
            
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // loads all attempts that belong to one specific quiz
    public List<Attempt> getAttemptsForQuiz(int quizId) {
        List<Attempt> attempts = new ArrayList<>();
        String sql = "SELECT * FROM attempts WHERE quiz_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapRowToAttempt(conn, rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attempts;
    }

    // returns all attempts made by a particular user
    public List<Attempt> getAttemptsForUser(String username) {
        List<Attempt> attempts = new ArrayList<>();
        String sql = "SELECT * FROM attempts WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapRowToAttempt(conn, rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attempts;
    }

    // finds a spe attempt using a "mockID" created from username+quizid
    public Attempt findAttemptByMockId(int mockAttemptId, String username) {
        // Find all attempts for user and then check mock IDs
        List<Attempt> userAttempts = getAttemptsForUser(username);
        int usernameHash = username.hashCode();
        
        for (Attempt attempt : userAttempts) {
            int calcMockId = usernameHash + attempt.getQuizId();
            if (calcMockId == mockAttemptId) {
                return attempt;
            }
        }
        return null;
    }
    
    private Attempt mapRowToAttempt(Connection conn, ResultSet rs) throws Exception {
        Attempt a = new Attempt();
        a.setId(rs.getInt("id"));
        a.setUsername(rs.getString("username"));
        a.setQuizId(rs.getInt("quiz_id"));
        a.setScore(rs.getInt("score"));
        a.setTotal(rs.getInt("total"));
        a.setTimestamp(rs.getString("timestamp"));
        
        Map<Integer, Integer> answers = new HashMap<>();
        String sql = "SELECT question_id, submitted_answer FROM attempt_answers WHERE attempt_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, a.getId());
            try (ResultSet ansRs = stmt.executeQuery()) {
                while (ansRs.next()) {
                    answers.put(ansRs.getInt("question_id"), ansRs.getInt("submitted_answer"));
                }
            }
        }
        a.setAnswers(answers);
        
        return a;
    }
}
