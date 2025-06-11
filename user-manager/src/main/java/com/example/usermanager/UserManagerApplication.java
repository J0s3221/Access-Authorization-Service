package com.example.usermanager;

import com.example.usermanager.entity.User;
import com.example.usermanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

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

        // CLI loop
        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("1. List all users");
            System.out.println("2. Add a user");
            System.out.println("3. Delete a user");
            System.out.println("4. Find user by ID");
            System.out.println("5. Exit");
            System.out.println("6. Generate two mock users");

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
                case 6:
                    System.out.println("Generate two mock users");
                    addUser("Alice");
                    addUser("Bob");
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // List all users
    private void listUsers() {
        userRepository.findAll().forEach(user ->
                System.out.println(user.getUsername() + " (ID: " + user.getId() + ", Email: " + user.getEmail() + ")"));
    }

    private void addUser(Scanner scanner) throws Exception {
        System.out.print("Enter username of the new user: ");
        String username = scanner.nextLine();

        // Gerar userId (UUID)
        String userId = UUID.randomUUID().toString();

        // Gerar chave simétrica AES 256 bits em Base64
        String symKey = generateSymmetricKey();

        // Gerar email falso
        String email = generateRandomEmail(username);

        User newUser = new User(symKey, username, email);
        userRepository.save(newUser);

        System.out.println("User added: " + newUser.getUsername() + " with email " + email);
        System.out.println("Symmetric key (base64): " + symKey);
    }

    private void addUser(String username) throws Exception {
        // Gerar userId (UUID)
        String userId = UUID.randomUUID().toString();

        // Gerar chave simétrica AES 256 bits em Base64
        String symKey = generateSymmetricKey();

        // Gerar email falso
        String email = generateRandomEmail(username);

        User newUser = new User(symKey, username, email);
        userRepository.save(newUser);

        System.out.println("User added: " + newUser.getUsername() + " with email " + email);
        System.out.println("Symmetric key (base64): " + symKey);
    }

    // Delete a user by ID
    private void deleteUser(Scanner scanner) {
        System.out.print("Enter user ID to delete: ");
        Long userId = scanner.nextLong();
        scanner.nextLine(); // consume newline

        Optional<User> user = userRepository.findById(userId);

        if (user.isPresent()) {
            userRepository.delete(user.get());
            System.out.println("User deleted: " + user.get().getUsername());
        } else {
            System.out.println("User with ID " + userId + " not found.");
        }
    }

    // Find a user by ID
    private void findUserById(Scanner scanner) {
        System.out.print("Enter user ID to find: ");
        Long userId = scanner.nextLong();
        scanner.nextLine(); // consume newline

        Optional<User> user = userRepository.findById(userId);

        if (user.isPresent()) {
            System.out.println("User found: " + user.get());
        } else {
            System.out.println("User with ID " + userId + " not found.");
        }
    }

    // Helper to generate random email based on name
    private String generateRandomEmail(String name) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String cleanName = name.toLowerCase().replaceAll("\\s+", "");
        return cleanName + uuid + "@example.com";
    }

    // Helper to generate symmetric AES key encoded in base64
    private String generateSymmetricKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // 128-bit AES
        SecretKey secretKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}
