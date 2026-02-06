package org.example;

public class App {
    public static void main(String[] args) {
        System.out.println("Testing Database Connection...");
        Database.connect();


        ProfileManager manager = ProfileManager.loadState();

        ProfileServer server = new ProfileServer(manager);
        server.start();

        manager.startAutoSaveThread();
        manager.startInteractiveConsole();
        manager.saveState();
    }
}