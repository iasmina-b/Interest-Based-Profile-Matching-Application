package org.example;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProfileTest {

    @Test
    void testProfileConstructor() {
        Interest interest = new Interest("Hiking");
        List<MatchPreference> prefs = new ArrayList<>();
        prefs.add(new MatchPreference(18, 25));

        Profile profile = new Profile("Alice", 25, interest, prefs);

        assertEquals("Alice", profile.getUsername());
        assertEquals(25, profile.getAge());
        assertEquals("Hiking", profile.getPrimaryInterest().getName());
        assertFalse(profile.getPreferences().isEmpty());
    }


    @Test
    void testSetUsernameFunctionality() {
        Interest interest = new Interest("Gaming");
        List<MatchPreference> prefs = new ArrayList<>();
        Profile profile = new Profile("OldName", 30, interest, prefs);

        profile.setUsername("NewName");

        assertEquals("NewName", profile.getUsername());
    }

    @Test
    void testCompareToFunctionality() {
        Interest interest = new Interest("Reading");
        List<MatchPreference> prefs = new ArrayList<>();

        Profile p1 = new Profile("Aaron", 20, interest, prefs);
        Profile p2 = new Profile("Zack", 20, interest, prefs);

        assertTrue(p1.compareTo(p2) < 0, "Aaron should be 'less than' Zack");
        assertTrue(p2.compareTo(p1) > 0, "Zack should be 'greater than' Aaron");
    }

    @Test
    void testToStringFormatting() {
        Interest interest = new Interest("Cooking");
        List<MatchPreference> prefs = new ArrayList<>();
        Profile profile = new Profile("ChefBob", 40, interest, prefs);

        String result = profile.toString();

        assertTrue(result.contains("ChefBob"));
        assertTrue(result.contains("Age: 40"));
        assertTrue(result.contains("Primary: Cooking"));
    }
}