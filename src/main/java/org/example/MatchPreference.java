package org.example;

public class MatchPreference {
    private int minAge;
    private int maxAge;

    public MatchPreference(int minAge, int maxAge) {
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public void setMinAge(int minAge) { this.minAge = minAge; }
    public void setMaxAge(int maxAge) { this.maxAge = maxAge; }

    public int getMinAge() { return minAge; }
    public int getMaxAge() { return maxAge; }

    @Override
    public String toString() {
        return "Age: " + minAge + "-" + maxAge + "]";
    }
}