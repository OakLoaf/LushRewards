package me.dave.lushrewards.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.Function;

@SuppressWarnings("unused")
public class StorageObject {
    private final String key;
    private final String providerName;
    private final HashMap<String, StorageValue<?, ?>> values = new HashMap<>();

    public StorageObject(String key, @Nullable String providerName) {
        this.key = key;
        this.providerName = providerName;
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @Nullable
    public String getProviderName() {
        return providerName;
    }

    public String getString(String id) {
        return getObject(id, String.class);
    }

    public String getString(String id, String def) {
        return getObject(id, String.class, def);
    }

    public Integer getInteger(String id) {
        return getObject(id, Integer.class);
    }

    public Integer getInteger(String id, int def) {
        return getObject(id, Integer.class, def);
    }

    public Boolean getBoolean(String id) {
        return getObject(id, Boolean.class);
    }

    public Boolean getBoolean(String id, boolean def) {
        return getObject(id, Boolean.class, def);
    }

    public Double getDouble(String id) {
        return getObject(id, Double.class);
    }

    public Double getDouble(String id, double def) {
        return getObject(id, Double.class, def);
    }

    public Long getLong(String id) {
        return getObject(id, Long.class);
    }

    public Long getLong(String id, long def) {
        return getObject(id, Long.class, def);
    }

    public Object getObject(String id) {
        return values.get(id).localValue();
    }

    public Object getObject(String id, Object def) {
        return values.containsKey(id) ? values.get(id).localValue() : def;
    }

    public <T> T getObject(String id, Class<T> clazz) {
        return values.containsKey(id) ? clazz.cast(values.get(id).localValue()) : null;
    }

    public <T> T getObject(String id, Class<T> clazz, T def) {
        Object value = values.get(id).localValue();
        return clazz.isInstance(value) ? clazz.cast(value) : def;
    }

    public String getRemoteString(String id) {
        return getRemoteObject(id, String.class);
    }

    public String getRemoteString(String id, String def) {
        return getRemoteObject(id, String.class, def);
    }

    public Integer getRemoteInteger(String id) {
        return getRemoteObject(id, Integer.class);
    }

    public Integer getRemoteInteger(String id, int def) {
        return getRemoteObject(id, Integer.class, def);
    }

    public Boolean getRemoteBoolean(String id) {
        return getRemoteObject(id, Boolean.class);
    }

    public Boolean getRemoteBoolean(String id, boolean def) {
        return getRemoteObject(id, Boolean.class, def);
    }

    public Double getRemoteDouble(String id) {
        return getRemoteObject(id, Double.class);
    }

    public Double getRemoteDouble(String id, double def) {
        return getRemoteObject(id, Double.class, def);
    }

    public Long getRemoteLong(String id) {
        return getRemoteObject(id, Long.class);
    }

    public Long getRemoteLong(String id, long def) {
        return getRemoteObject(id, Long.class, def);
    }

    public Object getRemoteObject(String id) {
        return values.get(id).remoteValue();
    }

    public Object getRemoteObject(String id, Object def) {
        return values.containsKey(id) ? values.get(id).remoteValue() : def;
    }

    public <T> T getRemoteObject(String id, Class<T> clazz) {
        return values.containsKey(id) ? clazz.cast(values.get(id).remoteValue()) : null;
    }

    public <T> T getRemoteObject(String id, Class<T> clazz, T def) {
        Object value = values.get(id).remoteValue();
        return clazz.isInstance(value) ? clazz.cast(value) : def;
    }

    /**
     * Set a value
     * @param id Value's unique id
     * @param localValue Local value object
     * @param loadMethod Method to load object from remote to local type
     * @param prepareSaveMethod Method to prepare object from local to remote type
     * @param <T> Local Type
     * @param <S> Remote Type
     */
    public <T, S> void set(String id, T localValue, Function<S, T> loadMethod, Function<T, S> prepareSaveMethod) {
        values.put(id, new StorageValue<>(prepareSaveMethod.apply(localValue), loadMethod));
    }

    /**
     * Set a value (For remote values only)
     * @param id Value's unique id
     * @param remoteValue Remote value object
     * @param loadMethod Method to load object from remote to local type
     * @param <T> Local Type
     * @param <S> Remote Type
     */
    public <T, S> void set(String id, Object remoteValue, Function<S, T> loadMethod) {
        values.put(id, new StorageValue<>((S) remoteValue, loadMethod));
    }

    public HashMap<String, StorageValue<?, ?>> getValues() {
        return values;
    }

    public record StorageValue<T, S>(S remoteValue, Function<S, T> loadMethod) {
        public T localValue() {
            return loadMethod.apply(remoteValue);
        }
    }
}
