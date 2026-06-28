package com.example.quizapp.model;

import java.util.List;
import java.time.LocalDateTime;

public class Quiz {
    private int id; // ID is manual in the current logic, so no @GeneratedValue

    private String title;

    private int creatorUserId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String instructions;

    private List<Question> questions;

    public Quiz() {}

    public Quiz(int id, String title, int creatorUserId, LocalDateTime startTime, LocalDateTime endTime, String instructions) {
        this.id = id;
        this.title = title;
        this.creatorUserId = creatorUserId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.instructions = instructions;
    }
    
    // Legacy constructor
    public Quiz(int id, String title, int creatorUserId) {
        this.id = id;
        this.title = title;
        this.creatorUserId = creatorUserId;
    }


    public int getId() { return id; }
    public String getTitle() { return title; }
    public int getCreatorUserId() { return creatorUserId; }
    public List<Question> getQuestions() { return questions; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getInstructions() { return instructions; }


    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCreatorUserId(int creatorUserId) { this.creatorUserId = creatorUserId; }
    
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    
    //sets or replaces the list of questions in the quiz
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}