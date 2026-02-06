package org.example;

import java.sql.*;
import java.util.*;

public class ProfileManager implements Editable {
    private List<Profile> allProfiles;

    private static final List<Interest> AVAILABLE_INTERESTS = Arrays.asList(
            new Interest("Hiking"),
            new Interest("Gaming"),
            new Interest("Reading"),
            new Interest("Cooking"),
            new Interest("Fitness")
    );

    public ProfileManager() {
        this.allProfiles = new ArrayList<>();
    }

    // Load from database
    public static ProfileManager loadState() {
        ProfileManager manager = new ProfileManager();

        String queryProfiles = "SELECT username, age, primary_interest FROM profiles";
        String queryPrefs = "SELECT min_age, max_age FROM match_preferences WHERE profile_username = ?";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(queryProfiles)) {

            while (rs.next()) {
                String username = rs.getString("username");
                int age = rs.getInt("age");
                String interestName = rs.getString("primary_interest");

                List<MatchPreference> prefs = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(queryPrefs)) {
                    ps.setString(1, username);
                    ResultSet rsPrefs = ps.executeQuery();
                    while (rsPrefs.next()) {
                        prefs.add(new MatchPreference(rsPrefs.getInt("min_age"), rsPrefs.getInt("max_age")));
                    }
                }

                Profile p = new Profile(username, age, new Interest(interestName), prefs);
                manager.addProfile(p);
            }
            System.out.println("Application state loaded from Database.");

        } catch (SQLException e) {
            System.err.println("Error loading from DB: " + e.getMessage());
        }
        return manager;
    }

    public void saveState() {
        try (Connection conn = Database.connect()) {
            conn.createStatement().execute("DELETE FROM match_preferences");
            conn.createStatement().execute("DELETE FROM profiles");

            String insertProfile = "INSERT INTO profiles (username, age, primary_interest) VALUES (?, ?, ?)";
            String insertPref = "INSERT INTO match_preferences (profile_username, min_age, max_age) VALUES (?, ?, ?)";

            PreparedStatement psProfile = conn.prepareStatement(insertProfile);
            PreparedStatement psPref = conn.prepareStatement(insertPref);

            for (Profile p : allProfiles) {
                psProfile.setString(1, p.getUsername());
                psProfile.setInt(2, p.getAge());
                psProfile.setString(3, p.getPrimaryInterest().getName());
                psProfile.executeUpdate();

                for (MatchPreference pref : p.getPreferences()) {
                    psPref.setString(1, p.getUsername());
                    psPref.setInt(2, pref.getMinAge());
                    psPref.setInt(3, pref.getMaxAge());
                    psPref.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving to DB: " + e.getMessage());
        }
    }

    public void startInteractiveConsole() {
        Scanner scanner = new Scanner(System.in);
        String command;

        System.out.println("\n--- Profile Manager Console ---");
        System.out.println("Enter a command (type 'help' for options, 'exit' to quit).");

        while (true) {
            System.out.print(">> ");
            command = scanner.nextLine().trim().toLowerCase();

            if (command.isEmpty()) continue;

            switch (command) {
                case "create":
                    this.createNewProfileViaInput();
                    break;
                case "display":
                    this.displayAllProfiles();
                    break;
                case "rename":
                    System.out.print("Enter current username: ");
                    String oldName = scanner.nextLine();
                    System.out.print("Enter new username: ");
                    String newName = scanner.nextLine();
                    this.update(oldName, newName);
                    break;
                case "sort-user":
                    this.sortByUsername();
                    this.displayAllProfiles();
                    break;
                case "sort-age":
                    this.sortByAge();
                    this.displayAllProfiles();
                    break;
                case "group":
                    System.out.print("Group by which interest? ");
                    String interest = scanner.nextLine();
                    ProfileGroup group = this.groupProfilesByInterest(interest);
                    System.out.println("--- Group Results ---");
                    if (!group.getProfiles().isEmpty()) {
                        group.getProfiles().forEach(p -> System.out.println(" > " + p.getUsername()));
                    } else {
                        System.out.println("Group is empty.");
                    }
                    break;
                case "match":
                    this.findMatchesInteractive(scanner);
                    break;
                case "help":
                    printHelp();
                    break;
                case "search":
                    this.searchProfileDirectlyInDB(scanner);
                    break;
                case "exit":
                    this.saveState();
                    System.exit(0);
                    break;
                case "login":
                    System.out.println("\n--- Database Login ---");
                    System.out.println("1. Admin");
                    System.out.println("2. Guest");
                    System.out.print("Choose Role (1-2): ");

                    if (scanner.hasNextInt()) {
                        int role = scanner.nextInt();
                        scanner.nextLine();
                        if (role == 1) {
                            Database.setCredentials("admin", "admin123");
                        } else if (role == 2) {
                            Database.setCredentials("guest", "guest123");
                        } else {
                            System.out.println("Invalid choice.");
                        }
                    } else {
                        scanner.nextLine();
                    }
                    break;
                default:
                    System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  create      - Starts the interactive profile creation process.");
        System.out.println("  display     - Shows all managed profiles.");
        System.out.println("  search      - Searches for a specific user.");
        System.out.println("  rename      - Rename an existing profile's username.");
        System.out.println("  sort-user   - Sorts profiles by username.");
        System.out.println("  sort-age    - Sorts profiles by age.");
        System.out.println("  group       - Groups profiles by primary interest.");
        System.out.println("  match       - Find matches based on preferences.");
        System.out.println("  login       - Login to admin or guest.");
        System.out.println("  exit        - Saves state to Database and quits.");
    }

    private boolean usernameExists(String username) {
        for (Profile p : allProfiles) {
            if (p.getUsername().equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    public void addProfile(Profile p) {
        this.allProfiles.add(p);
    }

    public List<Profile> getAllProfiles() {
        return allProfiles;
    }

    @Override
    public void update(String usernameToFind, String newUsername) {
        for (Profile p : allProfiles) {
            if (p.getUsername().equals(usernameToFind)) {
                p.setUsername(newUsername);
                System.out.println("Renamed profile '" + usernameToFind + "' to '" + newUsername + "'.");
                return;
            }
        }
        System.out.println("Profile '" + usernameToFind + "' not found for update.");
    }

    private Interest selectPrimaryInterest(Scanner scanner) {
        int choice = -1;
        while (choice < 1 || choice > AVAILABLE_INTERESTS.size()) {
            System.out.println("\nSelect your Primary Interest:");
            for (int i = 0; i < AVAILABLE_INTERESTS.size(); i++) {
                System.out.println(" " + (i + 1) + ". " + AVAILABLE_INTERESTS.get(i).getName());
            }
            System.out.print("Enter choice (number 1-" + AVAILABLE_INTERESTS.size() + "): ");
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
                if (choice >= 1 && choice <= AVAILABLE_INTERESTS.size()) {
                    return AVAILABLE_INTERESTS.get(choice - 1);
                }
            } else {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
            }
        }
        return AVAILABLE_INTERESTS.get(0);
    }

    public void createNewProfileViaInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n--- Starting New Profile Creation ---");
        try {
            System.out.print("Enter Username: ");
            String username = scanner.nextLine().trim();
            if (username.isEmpty()) throw new IllegalArgumentException("Username cannot be empty.");
            if (usernameExists(username)) throw new DuplicateProfileException(username);

            System.out.print("Enter Age: ");
            int age = scanner.nextInt();
            scanner.nextLine();
            if (age < 0 || age > 150) throw new InvalidAgeException(age);

            Interest primaryInterest = selectPrimaryInterest(scanner);
            MatchPreference defaultPref = new MatchPreference(20, 35);
            Profile newProfile = new Profile(username, age, primaryInterest, Arrays.asList(defaultPref));
            this.addProfile(newProfile);
            System.out.println("Profile created successfully for: " + username);

        } catch (Exception e) {
            System.err.println("\n" + e.getMessage());
        }
    }

    public void displayAllProfiles() {
        if (allProfiles.isEmpty()) {
            System.out.println("\n--- No Profiles Loaded ---");
            return;
        }
        System.out.println("\n--- All Managed Profiles (" + allProfiles.size() + ") ---");
        for (Profile profile : allProfiles) {
            System.out.println(" * " + profile.toString());
        }
        System.out.println("--------------------------------------");
    }

    public void sortByUsername() {
        Collections.sort(this.allProfiles);
        System.out.println("Profiles sorted successfully by Username.");
    }

    public void sortByAge() {
        Collections.sort(this.allProfiles, Comparator.comparingInt(Profile::getAge));
        System.out.println("Profiles sorted successfully by Age.");
    }

    public ProfileGroup groupProfilesByInterest(String targetInterest) {
        ProfileGroup group = new ProfileGroup(targetInterest + " Group");
        for (Profile p : allProfiles) {
            if (p.getPrimaryInterest().getName().equalsIgnoreCase(targetInterest)) {
                group.addProfile(p);
            }
        }
        return group;
    }

    public void startAutoSaveThread() {
        Thread autoSaveThread = new Thread(() -> {
            System.out.println("[System] Auto-save feature started.");
            while (true) {
                try {
                    Thread.sleep(15000);
                    this.saveState();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        autoSaveThread.setDaemon(true);
        autoSaveThread.start();
    }

    public void findMatchesInteractive(Scanner scanner) {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        Profile seeker = null;
        for (Profile p : allProfiles) {
            if (p.getUsername().equalsIgnoreCase(username)) {
                seeker = p;
                break;
            }
        }

        if (seeker == null) {
            System.out.println("User '" + username + "' not found.");
            return;
        }

        try {
            System.out.print("Min Age: ");
            int min = scanner.nextInt();
            System.out.print("Max Age: ");
            int max = scanner.nextInt();
            scanner.nextLine();

            if (seeker.getAge() >= 18) {
                if (min < 18 || max < 18) {
                    System.out.println("Error: Age restriction. You must be under 18 to get matches in this age range.");
                    System.out.println("Match cancelled.");
                    return;
                }
            }

            MatchPreference pref = seeker.getPreferences().get(0);
            pref.setMinAge(min);
            pref.setMaxAge(max);


            System.out.println("\n--- Matches (Age " + min + "-" + max + ") ---");
            boolean found = false;

            for (Profile p : allProfiles) {
                if (!p.getUsername().equals(seeker.getUsername()) &&
                        p.getAge() >= min && p.getAge() <= max) {

                    System.out.println(" [MATCH] " + p.toString());
                    found = true;
                }
            }

            if (!found) System.out.println("No matches found.");

        } catch (Exception e) {
            System.out.println("Invalid input. Please enter numbers for age.");
            scanner.nextLine();
        }
    }

    public void searchProfileDirectlyInDB(Scanner scanner) {
        System.out.print("Enter username to search in DB: ");
        String searchName = scanner.nextLine().trim();

        String sql = "SELECT p.username, p.age, p.primary_interest " +
                "FROM profiles p " +
                "WHERE p.username = ?";

        try (java.sql.Connection conn = Database.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, searchName);

            java.sql.ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String foundName = rs.getString("username");
                int foundAge = rs.getInt("age");
                String foundInterest = rs.getString("primary_interest");

                System.out.println("\n[Database Result] Found Profile:");
                System.out.println("---------------------------------");
                System.out.println(" Username : " + foundName);
                System.out.println(" Age      : " + foundAge);
                System.out.println(" Interest : " + foundInterest);
                System.out.println("---------------------------------");
            } else {
                System.out.println("[Database Result] No profile found with name '" + searchName + "'.");
            }

        } catch (java.sql.SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public boolean updateProfileAge(String username, int newAge) {
        String sql = "UPDATE profiles SET age = ? WHERE username = ?";

        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newAge);
            pstmt.setString(2, username);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                for (Profile p : allProfiles) {
                    if (p.getUsername().equalsIgnoreCase(username)) {
                        p.setAge(newAge);
                        break;
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Update failed: " + e.getMessage());
        }
        return false;
    }
    public boolean deleteProfile(String username) {
        boolean removed = allProfiles.removeIf(p -> p.getUsername().equalsIgnoreCase(username));
        if (removed) {
            saveState();
        }
        return removed;
    }
}