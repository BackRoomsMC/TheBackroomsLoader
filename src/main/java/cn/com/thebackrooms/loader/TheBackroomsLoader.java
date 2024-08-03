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
import java.util.Objects;
import java.util.jar.JarFile;

public class TheBackroomsLoader {

    private static final String MOD_LOADER_URL = "https://api.thebackrooms.com.cn/info/mod_loader";
    private static final Path MOD_LOADER_PATH = Path.of("").resolve(".thebackrooms").resolve("mod_loader.jar");

    public static void premain(String agentArgs, Instrumentation inst) {
        try (HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.getDefault()).build()) {
            String[] info = client.send(HttpRequest.newBuilder(URI.create(MOD_LOADER_URL)).GET().build(), HttpResponse.BodyHandlers.ofString()).body().split("\\|");
            if (Files.notExists(MOD_LOADER_PATH) || !checkHash(MOD_LOADER_PATH, info[0])) {
                System.out.println("Mod Loader not found or hash mismatch, downloading...");
                Files.deleteIfExists(MOD_LOADER_PATH);
                if (Files.notExists(MOD_LOADER_PATH.getParent()))
                    Files.createDirectories(MOD_LOADER_PATH.getParent());
                try {
                    client.send(HttpRequest.newBuilder(URI.create(info[1])).GET().build(), HttpResponse.BodyHandlers.ofFile(MOD_LOADER_PATH));
                } catch (Throwable t) {
                    showDialog("无法连接至TheBackrooms API, 请检查你的网络连接: " + t);
                    System.exit(1);
                }
                if (!checkHash(MOD_LOADER_PATH, info[0]))
                    throw new IllegalStateException("文件下载失败, 请检查你的网络连接!");
            }

            inst.appendToSystemClassLoaderSearch(new JarFile(MOD_LOADER_PATH.toFile()));
            System.out.println("Launching Mod Loader...");
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
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(Files.readAllBytes(path));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes)
            hex.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return Objects.equals(hash, hex.toString());
    }

    private static void showDialog(String message) {
        if (!GraphicsEnvironment.isHeadless())
            return;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, message, "TheBackrooms Loader", JOptionPane.ERROR_MESSAGE);
    }


}