package com.example.mototripeval.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class User {

    @Id @GeneratedValue
    private Long id;

    private String name;
    private boolean premium;
    private int points;

    public User() {}

    public User(String name, boolean premium) {
        this.name = name;
        this.premium = premium;
        this.points = 0;
    }

    public void addPoints(int pts) {
        this.points += pts;
    }

    public boolean canJoinPremium() {
        return premium;
    }

}
