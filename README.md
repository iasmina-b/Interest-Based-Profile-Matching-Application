# Interest-Based-Profile-Matching-Application

## Overview

This application is a client-server system designed to connect users based on shared interests and age preferences.

The system consists of:
- A Java backend that manages application logic and data persistence
- A React frontend that provides role-based user interfaces
- A PostgreSQL database for storing user profiles and match preferences

The application supports two roles:
- User (Guest): Standard user interaction with the system
- Admin: System management and monitoring

---

## System Requirements

- Java Development Kit (JDK) version 11 or higher
- Node.js and npm for running the frontend
- PostgreSQL database server running on port 5432
- Recommended IDEs:
  - IntelliJ IDEA (Backend)
  - Visual Studio Code (Frontend)

---

## Setup Instructions

### Database Setup

1. Open pgAdmin or a PostgreSQL terminal.
2. Ensure a database named `postgres` exists.
3. Open the Query Tool.
4. Run the database initialization script provided at the end of this README.

This script will:
- Create the required database tables
- Configure a `guest` database user with the correct permissions

---

### Backend Setup

1. Open the backend project in IntelliJ IDEA.
2. Navigate to: src/main/java/org/example/App.java
3. Run the `main` method to start the backend application.

---

### Frontend Setup

1. Open a terminal in the project’s root directory.
2. Run: npm start


3. The application will automatically open in your default web browser.

---

## Application Usage Guide

The application is divided into three main scenes.

### Scene 1: Login and Role Selection

Upon launching the application, users are presented with a welcome screen.

Options:
- **Enter as User**: Logs in with guest privileges (standard user)
- **Admin Dashboard**: Logs in with administrator privileges

---

### Scene 2: User Dashboard (Client View)

The User Dashboard allows general users to interact with the community.

#### Browse Profiles
- View a list of all registered users
- Search users by username or interest
- Edit or rename your username using the edit icon next to your name

#### Join (Create Profile)
- Enter a unique username, age, and primary interest
- Click "Join Now" to save the profile to the database

#### Match
- Enter your username to identify yourself
- Set minimum and maximum age preferences
- View a list of compatible users that match your criteria

---

### Scene 3: Admin Dashboard (Manager View)

The Admin Dashboard is used to monitor and manage the system.

Features:
- System statistics showing total profiles and database connection status
- Role indicator confirming connection via the admin database role
- User management table displaying all registered users
- Force delete option allowing admins to permanently remove users from the database

---

## Database Initialization Script

```sql
CREATE TABLE IF NOT EXISTS profiles (
 username VARCHAR(50) PRIMARY KEY,
 age INT,
 primary_interest VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS match_preferences (
 id SERIAL PRIMARY KEY,
 profile_username VARCHAR(50) REFERENCES profiles(username)
     ON DELETE CASCADE,
 min_age INT,
 max_age INT
);

DO
$do$
BEGIN
 IF NOT EXISTS (
     SELECT FROM pg_catalog.pg_roles
     WHERE rolname = 'guest'
 ) THEN
     CREATE USER guest WITH PASSWORD 'guest123';
 END IF;
END
$do$;

GRANT CONNECT ON DATABASE postgres TO guest;
GRANT USAGE ON SCHEMA public TO guest;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO guest;
GRANT SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO guest;
