package net.HearthianDev.redstoneoverpower.block.enums;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum LogicalComparatorMode implements StringRepresentable {
        AND("and"),
        OR("or"),
        XOR("xor"),
        NAND("nand"),
        NOR("nor"),
        XNOR("xnor");

        private final String name;

        LogicalComparatorMode(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

        public @NonNull String getSerializedName() {
            return this.name;
        }
}
