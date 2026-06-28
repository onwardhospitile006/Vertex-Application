package com.example.quizapp.service;

import com.example.quizapp.model.Question;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {

    private final DataSource dataSource;

    public QuizService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createQuizWithId(int id,
                                 String title,
                                 int creatorId,
                                 String startDateTime,
                                 String endDateTime,
                                 String instructions) {
        String sql = "INSERT INTO quizzes (id, title, creator_user_id, start_time, end_time, instructions) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, title);
            stmt.setInt(3, creatorId);
            stmt.setString(4, startDateTime);
            stmt.setString(5, endDateTime);
            stmt.setString(6, instructions);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // returns a list of all quiz metadata-basically shows all the quizzes created so far
    public List<Map<String, Object>> listQuizMeta() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM quizzes";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRowToQuizMeta(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // adds a single question to the given quiz
    // id is assigned automatically by MySQL AUTO_INCREMENT - no manual id management needed
    public void addQuestion(int quizId, Question q) {
        String sql = "INSERT INTO questions (quiz_id, text, option_a, option_b, option_c, option_d, correct_index) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setString(2, q.getText());
            stmt.setString(3, q.getOptionA());
            stmt.setString(4, q.getOptionB());
            stmt.setString(5, q.getOptionC());
            stmt.setString(6, q.getOptionD());
            stmt.setInt(7, q.getCorrectIndex());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // fetch all the questions belonging to a particular quiz
    public List<Question> getQuestions(int quizId) {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE quiz_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question(
                            rs.getInt("id"),
                            rs.getString("text"),
                            rs.getString("option_a"),
                            rs.getString("option_b"),
                            rs.getString("option_c"),
                            rs.getString("option_d"),
                            rs.getInt("correct_index")
                    );
                    q.setQuizId(rs.getInt("quiz_id"));
                    questions.add(q);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return questions;
    }

    // completely replace all the questions of a quiz with a new set
    // id is omitted from INSERT - MySQL AUTO_INCREMENT assigns unique ids automatically
    public void overwriteQuestions(int quizId, List<Question> questions) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // start transaction

            // Delete all old questions for the quiz
            String delSql = "DELETE FROM questions WHERE quiz_id = ?";
            try (PreparedStatement delStmt = conn.prepareStatement(delSql)) {
                delStmt.setInt(1, quizId);
                delStmt.executeUpdate();
            }

            // Re-insert remaining questions; MySQL auto-assigns new unique ids
            String insertSql = "INSERT INTO questions (quiz_id, text, option_a, option_b, option_c, option_d, correct_index) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (Question q : questions) {
                    insertStmt.setInt(1, quizId);
                    insertStmt.setString(2, q.getText());
                    insertStmt.setString(3, q.getOptionA());
                    insertStmt.setString(4, q.getOptionB());
                    insertStmt.setString(5, q.getOptionC());
                    insertStmt.setString(6, q.getOptionD());
                    insertStmt.setInt(7, q.getCorrectIndex());
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // checks if the given user is actually the creator of the quiz
    public boolean isCreator(int quizId, int userId) {
        String sql = "SELECT creator_user_id FROM quizzes WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("creator_user_id") == userId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // retrieves metadata for a single quiz by its ID
    public Map<String, Object> getQuizMeta(int quizId) {
        String sql = "SELECT * FROM quizzes WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToQuizMeta(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Map<String, Object> mapRowToQuizMeta(ResultSet rs) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rs.getInt("id"));
        map.put("title", rs.getString("title"));
        map.put("creatorId", rs.getInt("creator_user_id"));
        map.put("start", rs.getString("start_time").replace(" ", "T"));
        map.put("end", rs.getString("end_time").replace(" ", "T"));
        map.put("instructions", rs.getString("instructions"));
        return map;
    }

}
