package com.example.usermanager;

import com.example.usermanager.entity.User;
import com.example.usermanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Optional;
import java.util.Scanner;

@SpringBootApplication
public class UserManagerApplication implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    public static void main(String[] args) {
        SpringApplication.run(UserManagerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Mock initialization of users
        if (userRepository.count() == 0) {
            System.out.println("Initializing users...");
            userRepository.save(new User("Alice", "publicKey1"));
            userRepository.save(new User("Bob", "publicKey2"));
        } else {
            System.out.println("Users already initialized.");
        }
        // Show initialized users
        System.out.println("Users initialized:");
        listUsers();

        // CLI loop
        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("1. List all users");
            System.out.println("2. Add a user");
            System.out.println("3. Delete a user");
            System.out.println("4. Find user by ID");
            System.out.println("5. Exit");

            int choice = scanner.nextInt();
            scanner.nextLine(); // consume the newline character

            switch (choice) {
                case 1:
                    System.out.println("Listing all users:");
                    listUsers();
                    break;
                case 2:
                    System.out.println("Adding a new user:");
                    addUser(scanner);
                    break;
                case 3:
                    System.out.println("Deleting a user:");
                    deleteUser(scanner);
                    break;
                case 4:
                    System.out.println("Finding a user by ID:");
                    findUserById(scanner);
                    break;
                case 5:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // List all users
    private void listUsers() {
        userRepository.findAll().forEach(user -> System.out.println(user.getName() + " (ID: " + user.getId() + ")"));
    }

    // Add a user
    private void addUser(Scanner scanner) {
        System.out.print("Enter name of the new user: ");
        String name = scanner.nextLine();
        System.out.print("Enter public key for " + name + ": ");
        String publicKey = scanner.nextLine();

        User newUser = new User(name, publicKey);
        userRepository.save(newUser);
        System.out.println("User added: " + newUser.getName());
    }

    // Delete a user by ID
    private void deleteUser(Scanner scanner) {
        System.out.print("Enter user ID to delete: ");
        Long userId = scanner.nextLong();
        Optional<User> user = userRepository.findById(userId);
        
        if (user.isPresent()) {
            userRepository.delete(user.get());
            System.out.println("User deleted: " + user.get().getName());
        } else {
            System.out.println("User with ID " + userId + " not found.");
        }
    }

    // Find a user by ID
    private void findUserById(Scanner scanner) {
        System.out.print("Enter user ID to find: ");
        Long userId = scanner.nextLong();
        Optional<User> user = userRepository.findById(userId);
        
        if (user.isPresent()) {
            System.out.println("User found: " + user.get());
        } else {
            System.out.println("User with ID " + userId + " not found.");
        }
    }
}
