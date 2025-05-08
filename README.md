# Access-Authorization-Service
## overview of the project
This is a small application designed to interact with a remote client. The client requests access, and the Access Authorization (AA) server replies with a green light (approval) or a red light (rejection).

Communication between the AA server and the client is established using a socket on each end. The client sends an ID, and the server checks its presence in the database. If it is a valid ID, the server generates a challenge and sends it to the client. After the client responds, the AA server verifies the authenticity of the response and grants access (green light) if it is correct.

The server responds with a red light in two different cases: if the sent ID is not in the database (invalid ID) or if the client fails to solve the challenge correctly (incorrect symmetric key).

## protocol

## Challenge
