package org.example;

import java.util.ArrayList;
import java.util.List;

public class ProfileGroup implements Editable {
    private String groupName;
    private List<Profile> profilesInGroup;

    public ProfileGroup(String groupName) {
        this.groupName = groupName;
        this.profilesInGroup = new ArrayList<>();
    }

    public void addProfile(Profile p) {
        this.profilesInGroup.add(p);
    }

    public List<Profile> getProfiles() {
        return profilesInGroup;
    }

    @Override
    public void update(String identifier, String newValue) {
        if (newValue.equalsIgnoreCase("remove")) {
            boolean removed = profilesInGroup.removeIf(p -> p.getUsername().equals(identifier));
            if (removed) {
                System.out.println("Group '" + groupName + "': Removed profile '" + identifier + "'.");
            } else {
                System.out.println("Group '" + groupName + "': Profile '" + identifier + "' not found.");
            }
        } else {
            System.out.println("Group update: Action not recognized.");
        }
    }
}