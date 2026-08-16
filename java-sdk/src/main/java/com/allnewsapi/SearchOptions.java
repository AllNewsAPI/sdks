package com.allnewsapi;

import java.util.List;

/**
 * Search options for the search and headlines endpoints.
 * Uses the builder pattern for fluent construction.
 */
public class SearchOptions {
    private String q;
    private String startDate;
    private String endDate;
    private Boolean content;
    private List<String> lang;
    private List<String> country;
    private List<String> region;
    private List<String> category;
    private Integer max;
    private List<String> attributes;
    private Integer page;
    private String sortby;
    private List<String> publisher;
    private String format;
    private String aiSentiment;
    private String aiEntityName;
    private String aiEntityType;

    private SearchOptions() {
    }

    public String getQ() {
        return q;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public Boolean getContent() {
        return content;
    }

    public List<String> getLang() {
        return lang;
    }

    public List<String> getCountry() {
        return country;
    }

    public List<String> getRegion() {
        return region;
    }

    public List<String> getCategory() {
        return category;
    }

    public Integer getMax() {
        return max;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public Integer getPage() {
        return page;
    }

    public String getSortby() {
        return sortby;
    }

    public List<String> getPublisher() {
        return publisher;
    }

    public String getFormat() {
        return format;
    }

    public String getAiSentiment() {
        return aiSentiment;
    }

    public String getAiEntityName() {
        return aiEntityName;
    }

    public String getAiEntityType() {
        return aiEntityType;
    }

    /**
     * Creates a new builder for SearchOptions.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing SearchOptions instances.
     */
    public static class Builder {
        private final SearchOptions options;

        private Builder() {
            this.options = new SearchOptions();
        }

        public Builder q(String q) {
            options.q = q;
            return this;
        }

        public Builder startDate(String startDate) {
            options.startDate = startDate;
            return this;
        }

        public Builder endDate(String endDate) {
            options.endDate = endDate;
            return this;
        }

        public Builder content(Boolean content) {
            options.content = content;
            return this;
        }

        public Builder lang(List<String> lang) {
            options.lang = lang;
            return this;
        }

        public Builder country(List<String> country) {
            options.country = country;
            return this;
        }

        public Builder region(List<String> region) {
            options.region = region;
            return this;
        }

        public Builder category(List<String> category) {
            options.category = category;
            return this;
        }

        public Builder max(Integer max) {
            options.max = max;
            return this;
        }

        public Builder attributes(List<String> attributes) {
            options.attributes = attributes;
            return this;
        }

        public Builder page(Integer page) {
            options.page = page;
            return this;
        }

        public Builder sortby(String sortby) {
            options.sortby = sortby;
            return this;
        }

        public Builder publisher(List<String> publisher) {
            options.publisher = publisher;
            return this;
        }

        public Builder format(String format) {
            options.format = format;
            return this;
        }

        public Builder aiSentiment(String aiSentiment) {
            options.aiSentiment = aiSentiment;
            return this;
        }

        public Builder aiEntityName(String aiEntityName) {
            options.aiEntityName = aiEntityName;
            return this;
        }

        public Builder aiEntityType(String aiEntityType) {
            options.aiEntityType = aiEntityType;
            return this;
        }

        /**
         * Builds the SearchOptions instance.
         *
         * @return the constructed SearchOptions
         */
        public SearchOptions build() {
            return options;
        }
    }
}
