package com.allnewsapi;

import java.util.List;
import java.util.Map;

/**
 * Represents a news article returned by the API.
 */
public class Article {
    private String title;
    private String description;
    private String category;
    private String content;
    private String country;
    private String region;
    private String lang;
    private List<String> authors;
    private String aiSentiment;
    private Map<String, Double> aiSentimentScores;
    private List<Entity> aiEntities;
    private String aiSummary;
    private String url;
    private String image;
    private String publishedAt;
    private Source source;

    public Article() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getAiSentiment() {
        return aiSentiment;
    }

    public void setAiSentiment(String aiSentiment) {
        this.aiSentiment = aiSentiment;
    }

    public Map<String, Double> getAiSentimentScores() {
        return aiSentimentScores;
    }

    public void setAiSentimentScores(Map<String, Double> aiSentimentScores) {
        this.aiSentimentScores = aiSentimentScores;
    }

    public List<Entity> getAiEntities() {
        return aiEntities;
    }

    public void setAiEntities(List<Entity> aiEntities) {
        this.aiEntities = aiEntities;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }
}
