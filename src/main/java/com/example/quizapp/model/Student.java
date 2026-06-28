package com.example.quizapp.model;

public class Student extends User {
    public Student() {
        super();
    }
    public Student(int id, String username, String passwordHash, String role) {
        super(id, username, passwordHash, role);
    }
}
