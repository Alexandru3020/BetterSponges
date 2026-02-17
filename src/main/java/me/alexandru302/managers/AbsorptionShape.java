package me.alexandru302.managers;

import me.alexandru302.BetterSponges;
import org.jetbrains.annotations.NotNull;

public enum AbsorptionShape {
    DIAMOND,
    CUBE,
    SPHERE;

    @NotNull
    public static AbsorptionShape fromConfig(String raw) {
        if (raw == null || raw.isBlank()) return CUBE;

        try {
            return AbsorptionShape.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            BetterSponges.getInstance().getLogger().severe("Failed to get the absorption_shape config value:" + raw +
                    "Did you write it wrong or is it my fault");
            return CUBE;
        }
    }
}
