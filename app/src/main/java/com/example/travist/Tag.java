package com.example.travist;

public class Tag {
    public int id;
    public String name;

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}