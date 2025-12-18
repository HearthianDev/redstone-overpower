package net.HearthianDev.redstoneoverpower.block.enums;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum SculkChamberMode implements StringRepresentable {
    LISTEN("listen"),
    CHARGED("charged"),
    ISOLATED("isolated"),
    COOLDOWN("cooldown");

    private final String name;

    SculkChamberMode(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    public @NonNull String getSerializedName() {
        return this.name;
    }
}
