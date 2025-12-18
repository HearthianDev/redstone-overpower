package net.HearthianDev.redstoneoverpower.block.enums;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public enum PipeType implements StringRepresentable {
        NONE("none"),
        IN("input"),
        IN_HOPPER("input_hopper"),
        OUT("output");

        private final String name;

        PipeType(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

        public @NonNull String getSerializedName() {
            return this.name;
        }

        @Nullable
        public static PipeType getOpposite(PipeType value) {
            if (value == PipeType.IN) {
                return PipeType.OUT;
            }
            if (value == PipeType.OUT) {
                return PipeType.IN;
            }

            return null;
        }
}
