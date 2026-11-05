package org.newgen.launcher;

import android.util.Log;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;

public class LocalServer implements Runnable {
    private static final String TAG = "NEWGEN_SERVER";
    private final int port;
    private final File webRoot;
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public LocalServer(int port, File webRoot) {
        this.port = port;
        this.webRoot = webRoot;
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        new Thread(this, "LocalWebServer").start();
    }

    public synchronized void stop() {
        isRunning = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            Log.i(TAG, "Local server listening on http://127.0.0.1:" + port);
            while (isRunning) {
                Socket client = serverSocket.accept();
                handleClient(client);
            }
        } catch (IOException e) {
            if (isRunning) Log.e(TAG, "Server error", e);
        }
    }

    private void handleClient(final Socket socket) {
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 OutputStream out = socket.getOutputStream()) {
                String line = in.readLine();
                if (line == null) return;
                String[] parts = line.split(" ");
                if (parts.length < 2) return;
                String uri = parts[1];
                if (uri.contains("?")) uri = uri.substring(0, uri.indexOf("?"));
                if (uri.equals("/") || uri.isEmpty()) uri = "/index.html";

                File file = new File(webRoot, uri.replaceFirst("^/", ""));
                if (file.isDirectory()) file = new File(file, "index.html");

                if (!file.exists() || !file.canRead()) {
                    String notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
                    out.write(notFound.getBytes());
                    out.flush();
                    return;
                }

                String mime = URLConnection.guessContentTypeFromName(file.getName());
                if (mime == null) {
                    if (file.getName().endsWith(".js") || file.getName().endsWith(".tsx") || file.getName().endsWith(".ts")) {
                        mime = "application/javascript";
                    } else if (file.getName().endsWith(".css")) {
                        mime = "text/css";
                    } else if (file.getName().endsWith(".html")) {
                        mime = "text/html";
                    } else {
                        mime = "application/octet-stream";
                    }
                }

                String header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mime + "\r\n" +
                        "Content-Length: " + file.length() + "\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: close\r\n\r\n";
                out.write(header.getBytes());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = fis.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                out.flush();
            } catch (Exception ignored) {
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }).start();
    }
}
