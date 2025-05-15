# Access-Authorization-Service
## Overview of the project
This is a small application designed to interact with a remote client. The client requests access, and the Access Authorization (AA) server replies with a green light (approval) or a red light (rejection).

Communication between the AA server and the client is established using a socket on each end. The client sends an ID, and the server checks its presence in the database. If it is a valid ID, the server generates a challenge and sends it to the client. After the client responds, the AA server verifies the authenticity of the response and grants access (green light) if it is correct.

The server responds with a red light in two different cases: if the sent ID is not in the database (invalid ID) or if the client fails to solve the challenge correctly (incorrect symmetric key).

## Structure and Layers
This application is developed in Java and uses the Spring Boot framework.

- **Repository Layer** – Responsible for data access and persistence operations.
- **Domain Layer** – Contains the core business entities and models.
- **Service Layer** – Encapsulates the application logic and coordinates interactions between layers.
- **Controller Layer** – Exposes endpoints and handles incoming requests.


Additionally, the application includes a user interface component reserved for administrators, which allows direct interaction with the database (e.g., managing access keys).


![image](https://github.com/user-attachments/assets/e8958d29-23eb-4eb6-936e-a887722271f7)

## Protocol
The protocol of comunication of the application is straight forward and simple. 

Every user with granted access is first logged in the database by the admin team (using the application for access managing), the database has a list of pairs of id and public key. When a user is logged, a pair of symmetric keys and an id are created. The id and the public key are logged in the database and the user keeps their private key.

When the user contacts the database, firstly, they send their id, the database checks if the id is present in the database if so it creates a challenge and sends it back to the user. Then the user responds with a signature composed of the hash of the challenge in SHA-256 and the id ciphered with their private key. The AA server receives this signature and firstly calculates the hash of the challenge, then it decyphers the signature and compares it with the diggest that it calculated. 

If everything is right, then the request of the user for access was succeful and the AA server grants premission.

![image](https://github.com/user-attachments/assets/d2b008a6-c97e-4750-922c-e472f48513a9)

## Challenge
