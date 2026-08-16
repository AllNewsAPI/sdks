package com.allnewsapi;

/**
 * Represents the response from the usage endpoint.
 */
public class UsageResponse {
    private String plan;
    private int requestsUsed24Hours;
    private int requestsLimit24Hours;
    private int requestsRemaining24Hours;
    private int requestsUsed30Days;

    public UsageResponse() {
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public int getRequestsUsed24Hours() {
        return requestsUsed24Hours;
    }

    public void setRequestsUsed24Hours(int requestsUsed24Hours) {
        this.requestsUsed24Hours = requestsUsed24Hours;
    }

    public int getRequestsLimit24Hours() {
        return requestsLimit24Hours;
    }

    public void setRequestsLimit24Hours(int requestsLimit24Hours) {
        this.requestsLimit24Hours = requestsLimit24Hours;
    }

    public int getRequestsRemaining24Hours() {
        return requestsRemaining24Hours;
    }

    public void setRequestsRemaining24Hours(int requestsRemaining24Hours) {
        this.requestsRemaining24Hours = requestsRemaining24Hours;
    }

    public int getRequestsUsed30Days() {
        return requestsUsed30Days;
    }

    public void setRequestsUsed30Days(int requestsUsed30Days) {
        this.requestsUsed30Days = requestsUsed30Days;
    }
}
