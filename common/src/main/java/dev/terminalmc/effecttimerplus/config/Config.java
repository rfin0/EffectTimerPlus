package dev.terminalmc.effecttimerplus.config;

import com.google.gson.*;
import dev.terminalmc.effecttimerplus.EffectTimerPlus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Config {
    public final int version = 1;
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = EffectTimerPlus.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options
    // Not using Options subclass due to legacy config format

    public static final double defaultScale = 1.0F;
    public double scale = defaultScale;

    public static final double defaultPotencyScale = 1.0F;
    public double potencyScale = defaultPotencyScale;

    public static final double defaultTimerScale = 1.0F;
    public double timerScale = defaultTimerScale;

    public static final boolean defaultPotencyEnabled = true;
    public boolean potencyEnabled = defaultPotencyEnabled;

    public static final boolean defaultTimerEnabled = true;
    public boolean timerEnabled = defaultTimerEnabled;

    public static final boolean defaultTimerEnabledAmbient = false;
    public boolean timerEnabledAmbient = defaultTimerEnabledAmbient;

    public static final boolean defaultTimerWarnEnabled = true;
    public boolean timerWarnEnabled = defaultTimerWarnEnabled;

    public static final boolean defaultTimerFlashEnabled = true;
    public boolean timerFlashEnabled = defaultTimerFlashEnabled;

    public static final int defaultTimerWarnTime = 20;
    public int timerWarnTime = defaultTimerWarnTime;

    public static final int defaultPotencyColor = -1140850689;
    public int potencyColor = defaultPotencyColor;

    public static final boolean defaultPotencyShadow = true;
    public boolean potencyShadow = defaultPotencyShadow;

    public static final boolean defaultPotencyBack = false;
    public boolean potencyBack = defaultPotencyBack;

    public static final int defaultPotencyBackColor = -1140850689;
    public int potencyBackColor = defaultPotencyBackColor;

    public static final int defaultTimerColor = -1711276033;
    public int timerColor = defaultTimerColor;

    public static final int defaultTimerWarnColor = -65536;
    public int timerWarnColor = defaultTimerWarnColor;

    public static final boolean defaultTimerShadow = true;
    public boolean timerShadow = defaultTimerShadow;

    public static final boolean defaultTimerBack = false;
    public boolean timerBack = defaultTimerBack;

    public static final int defaultTimerBackColor = -1776213727;
    public int timerBackColor = defaultTimerBackColor;

    public static final Integer[] locations = {0, 1, 2, 3, 4, 5, 6, 7};

    public static final int defaultPotencyLocation = 2;
    public int potencyLocation = defaultPotencyLocation;

    public static final int defaultTimerLocation = 6;
    public int timerLocation = defaultTimerLocation;

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
        }
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (FileReader reader = new FileReader(file.toFile())) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            EffectTimerPlus.LOG.error("Unable to load config.", e);
            return null;
        }
    }

    public static void save() {
        if (instance == null) return;
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            EffectTimerPlus.LOG.error("Unable to save config.", e);
        }
    }
}
