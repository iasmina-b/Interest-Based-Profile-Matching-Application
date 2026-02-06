package org.example;
import java.util.List;

public class Profile implements Comparable<Profile> {
    private String username;
    private int age;

    private Interest primaryInterest;
    private List<MatchPreference> preferences;

    public Profile(String username, int age, Interest primaryInterest, List<MatchPreference> preferences) {
        this.username = username;
        this.age = age;
        this.primaryInterest = primaryInterest;
        this.preferences = preferences;
    }

    public String getUsername() { return username; }
    public int getAge() { return age; }
    public Interest getPrimaryInterest() { return primaryInterest; }
    public void setUsername(String username) { this.username = username; }

    public List<MatchPreference> getPreferences() { return preferences; }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public int compareTo(Profile other) {
        return this.username.compareTo(other.getUsername());
    }

    @Override
    public String toString() {
        return String.format("%-15s (Age: %d) | Primary: %s",
                username, age, primaryInterest.getName());
    }
}