package io.icker.factions.database;

import io.icker.factions.api.persistents.Relationship.Permissions;
import io.icker.factions.api.persistents.Relationship.Status;
import io.icker.factions.api.persistents.User.ChatMode;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.api.persistents.User.SoundMode;
import net.minecraft.nbt.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

public class SerializerRegistry {
    private static final HashMap<Class<?>, Serializer<?, ? extends Tag>> registry = new HashMap<Class<?>, Serializer<?, ? extends Tag>>();

    private static class Serializer<T, E extends Tag> {
        private final Function<T, E> serializer;
        private final Function<E, T> deserializer;
    
        public Serializer(Function<T, E> serializer, Function<E, T> deserializer) {
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        @SuppressWarnings("unchecked")
        public Tag serialize(Object value) {
            return serializer.apply((T) value);
        }

        @SuppressWarnings("unchecked")
        public T deserialize(Tag value) {
            return deserializer.apply((E) value);
        }
    }

    static {
        registry.put(byte.class, new Serializer<Byte, ByteTag>(val -> ByteTag.valueOf(val), el -> el.getAsByte()));
        registry.put(short.class, new Serializer<Short, ShortTag>(val -> ShortTag.valueOf(val), el -> el.getAsShort()));
        registry.put(int.class, new Serializer<Integer, IntTag>(val -> IntTag.valueOf(val), el -> el.getAsInt()));
        registry.put(long.class, new Serializer<Long, LongTag>(val -> LongTag.valueOf(val), el -> el.getAsLong()));
        registry.put(float.class, new Serializer<Float, FloatTag>(val -> FloatTag.valueOf(val), el -> el.getAsFloat()));
        registry.put(double.class, new Serializer<Double, DoubleTag>(val -> DoubleTag.valueOf(val), el -> el.getAsDouble()));
        registry.put(boolean.class, new Serializer<Boolean, ByteTag>(val -> ByteTag.valueOf(val), el -> el.getAsByte() != 0));

        registry.put(byte[].class, new Serializer<Byte[], ByteArrayTag>(val -> new ByteArrayTag(ArrayUtils.toPrimitive(val)), el -> ArrayUtils.toObject(el.getAsByteArray())));
        registry.put(int[].class, new Serializer<Integer[], IntArrayTag>(val -> new IntArrayTag(ArrayUtils.toPrimitive(val)), el -> ArrayUtils.toObject(el.getAsIntArray())));
        registry.put(long[].class, new Serializer<Long[], LongArrayTag>(val -> new LongArrayTag(ArrayUtils.toPrimitive(val)), el -> ArrayUtils.toObject(el.getAsLongArray())));

        registry.put(String.class, new Serializer<String, StringTag>(val -> StringTag.valueOf(val), el -> el.getAsString()));
        registry.put(UUID.class, new Serializer<UUID, IntArrayTag>(val -> NbtUtils.createUUID(val), el -> NbtUtils.loadUUID(el)));
        registry.put(SimpleContainer.class, createInventorySerializer(54));

        registry.put(ChatMode.class, createEnumSerializer(ChatMode.class));
        registry.put(SoundMode.class, createEnumSerializer(SoundMode.class));
        registry.put(Rank.class, createEnumSerializer(Rank.class));
        registry.put(Status.class, createEnumSerializer(Status.class));
        registry.put(Permissions.class, createEnumSerializer(Permissions.class));
    }

    public static boolean contains(Class<?> clazz) {
        return registry.containsKey(clazz);
    }

    public static <T> Tag toNbtElement(Class<T> clazz, T value) {        
        return registry.get(clazz).serialize(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromNbtElement(Class<T> clazz, Tag value) {        
        return (T) registry.get(clazz).deserialize(value);
    }

    private static <T extends Enum<T>> Serializer<T, StringTag> createEnumSerializer(Class<T> clazz) {
        return new Serializer<T, StringTag>(
            val -> StringTag.valueOf(val.toString()),
            el -> Enum.valueOf(clazz, el.getAsString())
        );
    }

    private static Serializer<SimpleContainer, ListTag> createInventorySerializer(int size) {
        return new Serializer<SimpleContainer, ListTag>(val -> {
            ListTag nbtList = new ListTag();

            for(int i = 0; i < val.getContainerSize(); ++i) {
                ItemStack itemStack = val.getItem(i);
                if (!itemStack.isEmpty()) {
                    CompoundTag nbtCompound = new CompoundTag();
                    nbtCompound.putByte("Slot", (byte) i);
                    itemStack.save(nbtCompound);
                    nbtList.add(nbtCompound);
                }
            }
    
            return nbtList;
        }, el -> {
            SimpleContainer inventory = new SimpleContainer(size);

            for(int i = 0; i < size; ++i) {
                inventory.setItem(i, ItemStack.EMPTY);
            }

            for(int i = 0; i < el.size(); ++i) {
                CompoundTag nbtCompound = el.getCompound(i);
                int slot = nbtCompound.getByte("Slot") & 255;
                if (slot >= 0 && slot < size) {
                    inventory.setItem(slot, ItemStack.of(nbtCompound));
                }
            }

            return inventory;
        });
    }
}