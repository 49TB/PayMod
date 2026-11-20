package com.paymentfx.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON-backed settings: master switch, volume, the five tier thresholds, per-tier toggles and the chat format. */
public final class PaymentFxConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("paymentfx");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String DEFAULT_CHAT_PATTERN = "{player} paid you ${amount}";

    private static PaymentFxConfig instance;

    public boolean enabled = true;
    public float masterVolume = 1.0F;

    /** Minimum amount that starts each tier; evaluated from tier 5 downwards. */
    public long tier1Min = 0L;
    public long tier2Min = 10_000_000L;
    public long tier3Min = 500_000_000L;
    public long tier4Min = 1_000_000_000L;
    public long tier5Min = 10_000_000_000L;

    public boolean tier1Enabled = true;
    public boolean tier2Enabled = true;
    public boolean tier3Enabled = true;
    public boolean tier4Enabled = true;
    public boolean tier5Enabled = true;

    /** Server chat line template; {@code {player}} and {@code {amount}} are replaced by regex groups. */
    public String chatPattern = DEFAULT_CHAT_PATTERN;

    public static PaymentFxConfig get() {
        if (instance == null) {
            instance = new PaymentFxConfig();
        }
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("paymentfx.json");
    }

    public static void load() {
        PaymentFxConfig loaded = new PaymentFxConfig();
        Path path = file();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                PaymentFxConfig parsed = GSON.fromJson(reader, PaymentFxConfig.class);
                if (parsed != null) {
                    loaded = parsed;
                }
            } catch (Exception exception) {
                LOGGER.warn("paymentfx.json could not be read, falling back to defaults", exception);
            }
        }
        normalize(loaded);
        instance = loaded;
        save();
    }

    public static void save() {
        PaymentFxConfig config = get();
        normalize(config);
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file())) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            LOGGER.warn("paymentfx.json could not be written", exception);
        }
    }

    /** Clamps volume, keeps the thresholds ascending and repairs a missing chat pattern. */
    public static void normalize(PaymentFxConfig config) {
        config.masterVolume = Math.max(0.0F, Math.min(1.0F, config.masterVolume));
        if (config.tier1Min < 0L) {
            config.tier1Min = 0L;
        }
        if (config.tier2Min <= config.tier1Min) {
            config.tier2Min = safeAdd(config.tier1Min, 1L);
        }
        if (config.tier3Min <= config.tier2Min) {
            config.tier3Min = safeAdd(config.tier2Min, 1L);
        }
        if (config.tier4Min <= config.tier3Min) {
            config.tier4Min = safeAdd(config.tier3Min, 1L);
        }
        if (config.tier5Min <= config.tier4Min) {
            config.tier5Min = safeAdd(config.tier4Min, 1L);
        }
        if (config.chatPattern == null || config.chatPattern.isBlank()) {
            config.chatPattern = DEFAULT_CHAT_PATTERN;
        }
    }

    private static long safeAdd(long value, long delta) {
        long result = value + delta;
        return result < value ? Long.MAX_VALUE / 2L : result;
    }
}
