package model;

/**
 * Represents a user in the users table.
 */
public class User {
    private int userId;
    private String name;
    private String email;
    private String password;      // SHA-256 hash, never the real password
    private int age;
    private String gender;        // "Male", "Female" or "Other"
    private double height;        // cm
    private double weight;        // kg
    private String activityLevel; // one of GoalCalculator.ACTIVITY_LEVELS

    public User() {
    }

    public User(int userId, String name, String email, String password, int age, String gender,
                double height, double weight, String activityLevel) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.age = age;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.activityLevel = activityLevel;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }
}
