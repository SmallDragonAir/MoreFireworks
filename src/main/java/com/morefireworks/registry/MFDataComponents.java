package com.morefireworks.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.morefireworks.MoreFireworks;
import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.SpecCodec;
import com.morefireworks.core.StarSpec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * The two custom data components that carry a firework's parameters.
 *
 * <p>They store the canonical text form from {@link SpecCodec}, so a component is a single string:
 * a few dozen bytes on disk and on the wire, no matter how exotic the firework is. Vanilla's
 * {@code minecraft:fireworks} component is kept in sync on the same stack so that vanilla clients,
 * vanilla tooltips and vanilla particles all keep working.</p>
 */
public final class MFDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MoreFireworks.MOD_ID);

    public static final Supplier<DataComponentType<RocketSpec>> ROCKET_SPEC =
            DATA_COMPONENTS.registerComponentType(
                    "rocket_spec",
                    builder -> builder
                            .persistent(specCodec(SpecCodec::decode, SpecCodec::encode))
                            .networkSynchronized(stringStreamCodec(SpecCodec::decode, SpecCodec::encode)));

    public static final Supplier<DataComponentType<StarSpec>> STAR_SPEC =
            DATA_COMPONENTS.registerComponentType(
                    "star_spec",
                    builder -> builder
                            .persistent(specCodec(SpecCodec::decodeStar, SpecCodec::encodeStar))
                            .networkSynchronized(stringStreamCodec(SpecCodec::decodeStar, SpecCodec::encodeStar)));

    private MFDataComponents() {
    }

    /**
     * Wraps a validating string parser/serialiser as a {@link Codec}. Parse failures become data
     * errors rather than exceptions, so a hostile NBT payload cannot crash the game.
     */
    private static <T> Codec<T> specCodec(java.util.function.Function<String, T> decoder,
                                          java.util.function.Function<T, String> encoder) {
        return Codec.STRING.flatXmap(
                text -> {
                    try {
                        return DataResult.success(decoder.apply(text));
                    } catch (RuntimeException e) {
                        return DataResult.error(() -> "Invalid firework specification: " + e.getMessage());
                    }
                },
                value -> DataResult.success(encoder.apply(value)));
    }

    private static <T> StreamCodec<ByteBuf, T> stringStreamCodec(
            java.util.function.Function<String, T> decoder,
            java.util.function.Function<T, String> encoder) {
        return new StreamCodec<>() {
            @Override
            public T decode(ByteBuf buf) {
                int length = buf.readUnsignedShort();
                byte[] bytes = new byte[length];
                buf.readBytes(bytes);
                return decoder.apply(new String(bytes, StandardCharsets.UTF_8));
            }

            @Override
            public void encode(ByteBuf buf, T value) {
                byte[] bytes = encoder.apply(value).getBytes(StandardCharsets.UTF_8);
                if (bytes.length > 0xFFFF) {
                    throw new IllegalArgumentException("firework specification too long to synchronise");
                }
                buf.writeShort(bytes.length);
                buf.writeBytes(bytes);
            }
        };
    }
}
