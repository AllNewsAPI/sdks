package com.allnewsapi;

/**
 * Represents an AI-extracted entity from a news article.
 */
public class Entity {
    private String name;
    private String type;

    public Entity() {
    }

    public Entity(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
