package cn.com.thebackrooms.loader;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Objects;
import java.util.jar.JarFile;

public class TheBackroomsLoader {

    private static final String MOD_LOADER_URL = "https://api.thebackrooms.com.cn/resources/mod-loader";
    private static final Path MOD_LOADER_PATH = Path.of("").resolve(".thebackrooms").resolve("mod_loader.jar");
    private static final int MAX_RETRIES = 3;
    private static final String VERSION = "1.2.0";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[TbMC-Loader] TheBackrooms Loader " + VERSION);
        try (HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .proxy(ProxySelector.getDefault())
                .build()) {
            System.out.println("[TbMC-Loader] Getting ModLoader Info...");
            String[] info = sendWithRetry(client, HttpRequest.newBuilder(URI.create(MOD_LOADER_URL)).GET().timeout(REQUEST_TIMEOUT).build(), HttpResponse.BodyHandlers.ofString()).split("\\|");
            if (Files.notExists(MOD_LOADER_PATH) || !checkHash(MOD_LOADER_PATH, info[0])) {
                System.out.println("[TbMC-Loader] Mod Loader not found or hash mismatch, downloading...");
                Files.deleteIfExists(MOD_LOADER_PATH);
                if (Files.notExists(MOD_LOADER_PATH.getParent()))
                    Files.createDirectories(MOD_LOADER_PATH.getParent());
                try {
                    sendWithRetry(client, HttpRequest.newBuilder(URI.create(info[1])).GET().timeout(REQUEST_TIMEOUT).build(), HttpResponse.BodyHandlers.ofFile(MOD_LOADER_PATH));
                } catch (Throwable t) {
                    showDialog("无法连接至 TheBackrooms API, 请检查你的网络连接: " + t);
                    t.printStackTrace();
                    System.exit(1);
                }
                if (!checkHash(MOD_LOADER_PATH, info[0]))
                    throw new IllegalStateException("文件下载失败, 请检查你的网络连接!");
            }

            inst.appendToSystemClassLoaderSearch(new JarFile(MOD_LOADER_PATH.toFile()));
            System.out.println("[TbMC-Loader] Launching Mod Loader...");
            Class.forName("cn.com.thebackrooms.modloader.noobf.TheBackroomsModLoader")
                    .getDeclaredMethod("premain", String.class, Instrumentation.class)
                    .invoke(null, agentArgs, inst);
        } catch (Throwable t) {
            showDialog("加载失败: " + t);
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean checkHash(Path path, String hash) throws NoSuchAlgorithmException, IOException {
        System.out.println("[TbMC-Loader] Checking ModLoader Hash...");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(Files.readAllBytes(path));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes)
            hex.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return Objects.equals(hash, hex.toString());
    }

    private static void showDialog(String message) {
        if (GraphicsEnvironment.isHeadless())
            return;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, message, "TheBackrooms Loader", JOptionPane.ERROR_MESSAGE);
    }

    private static <T> T sendWithRetry(HttpClient client, HttpRequest request, HttpResponse.BodyHandler<T> response) throws Exception {
        Exception last = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                System.out.println("[HttpClient] Try connect " + (i + 1) + "/" + MAX_RETRIES);
                HttpResponse<T> resp = client.send(request, response);
                if (resp.statusCode() == 200) {
                    return resp.body();
                }
                throw new IOException("HTTP " + resp.statusCode());
            } catch (Exception e) {
                last = e;
                System.out.println("[HttpClient] Failed to connect: " + e.getMessage());
                if (i < MAX_RETRIES - 1) {
                    Thread.sleep(1000L * (i + 1));
                }
            }
        }
        throw last;
    }
}
