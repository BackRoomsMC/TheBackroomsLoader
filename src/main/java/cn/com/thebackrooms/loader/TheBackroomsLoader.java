package cn.com.thebackrooms.loader;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
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
    private static final Path MOD_LOADER_PATH = Path.of(".thebackrooms").resolve("mod_loader.jar");

    public static void premain(String agentArgs, Instrumentation inst) throws Throwable {
        try (HttpClient client = HttpClient.newHttpClient()) {
            String[] info = client.send(HttpRequest.newBuilder(URI.create(MOD_LOADER_URL)).GET().build(), HttpResponse.BodyHandlers.ofString()).body().split("\\|");
            if (Files.notExists(MOD_LOADER_PATH) || !checkHash(MOD_LOADER_PATH, info[0]))
                client.send(HttpRequest.newBuilder(URI.create(info[1])).GET().build(), HttpResponse.BodyHandlers.ofFile(MOD_LOADER_PATH));

            inst.appendToSystemClassLoaderSearch(new JarFile(MOD_LOADER_PATH.toFile()));
            Class.forName("cn.com.thebackrooms.modloader.noobf.TheBackroomsModLoader")
                    .getDeclaredMethod("premain", String.class, Instrumentation.class)
                    .invoke(null, agentArgs, inst);
        } catch (Throwable t) {
            if (!GraphicsEnvironment.isHeadless())
                showDialog(t);
            throw t;
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

    private static void showDialog(Throwable t) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, "加载失败: " + t.toString(), "TheBackrooms Loader", JOptionPane.ERROR_MESSAGE);
    }


}