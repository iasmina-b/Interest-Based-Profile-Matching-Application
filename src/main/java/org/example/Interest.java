package org.example;

public class Interest implements Displayable, Comparable<Interest> {
    private String name;

    public Interest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getSummary() { return "Interest Name: " + this.name; }

    @Override
    public int compareTo(Interest other) {
        return this.name.compareTo(other.getName());
    }

    @Override
    public String toString() {
        return name;
    }
}