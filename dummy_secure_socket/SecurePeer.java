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
