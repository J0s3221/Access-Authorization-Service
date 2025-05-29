package dummy_secure_socket;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.*;

public class SecurePeer {

    public static void main(String[] args) throws Exception {
        Map<String, String> config = parseArgs(args);
        String keystorePath = config.getOrDefault("keystore", "keystore.jks");
        String password = config.getOrDefault("password", "changeit");
        String host = config.getOrDefault("host", "localhost");
        int port = Integer.parseInt(config.getOrDefault("port", "12345"));
        int pid = (int) ProcessHandle.current().pid();

        KeyStore ks = loadKeyStore(keystorePath, password.toCharArray());
        SSLContext sslContext = createSSLContext(ks, password.toCharArray());

        boolean isServer = isPortAvailable(port);

        if (isServer) {
            runServer(sslContext, port, pid);
        } else {
            Thread.sleep(1000); // give server a second to start
            runClient(sslContext, host, port, pid);
        }
    }

    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.contains("=")) {
                String[] parts = arg.replace("--", "").split("=", 2);
                map.put(parts[0], parts[1]);
            }
        }
        return map;
    }

    static boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static void runServer(SSLContext sslContext, int port, int pid) throws IOException {
        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port)) {
            System.out.println("[Server " + pid + "] Listening on port " + port + "...");
            try (SSLSocket socket = (SSLSocket) serverSocket.accept()) {
                talk(socket, pid);
            }
        }
    }

    static void runClient(SSLContext sslContext, String host, int port, int pid) throws IOException {
        SSLSocketFactory sf = sslContext.getSocketFactory();
        try (SSLSocket socket = (SSLSocket) sf.createSocket(host, port)) {
            talk(socket, pid);
        }
    }

    static void talk(SSLSocket socket, int pid) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.println("Hello from PID " + pid);
        String response = in.readLine();
        System.out.println("[PID " + pid + "] Received: " + response);

        //obter o challenge
        String id = "user123"; //ID válido
        String challenge = getChallengeFromSpring(id);
        if (challenge == null) {
            System.err.println("Challenge não obtido.");
            return;
        }
        System.out.println("Challenge recebido: " + challenge);

        //simula assinatura (no mundo real, seria feita com chave privada)
        String fakeSignature = "signed_" + challenge;

        //confirma assinatura
        String result = confirmSignatureWithSpring(fakeSignature, challenge);
        if (result != null) {
            System.out.println("Resposta do servidor: " + result);
        } else {
            System.err.println("Falha na autenticação.");
        }
    }

    static String getChallengeFromSpring(String id) {
        try {
            URL url = new URL("http://localhost:8080/access/challenge?id=" + URLEncoder.encode(id, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return reader.readLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao obter challenge: " + e.getMessage());
            return null;
        }
    }

    static String confirmSignatureWithSpring(String sign, String challenge) {
        try {
            URL url = new URL("http://localhost:8080/access/verify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonPayload = String.format("{\"sign\":\"%s\", \"challenge\":\"%s\"}", sign, challenge);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return reader.readLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao confirmar assinatura: " + e.getMessage());
            return null;
        }
    }

    static KeyStore loadKeyStore(String path, char[] password) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream is = new FileInputStream(path)) {
            ks.load(is, password);
        }
        return ks;
    }

    static SSLContext createSSLContext(KeyStore keyStore, char[] password) throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keyStore); // For demo: trust own cert. You can separate truststore.

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ctx;
    }
}
