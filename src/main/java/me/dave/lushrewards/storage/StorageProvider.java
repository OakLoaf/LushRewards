package me.dave.lushrewards.storage;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.utils.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class StorageProvider<T extends Keyed> {
    private final String name;
    private final Class<T> dataType;
    private final Function<String, T> collector;
    private final Function<StorageObject, T> loader;
    private final HashMap<String, MethodHolder<T, ?, ?>> methodHolders;

    /**
     * @param name Name of storage set (Must be unique)
     * @param dataType Type of provider
     * @param loader The method to load
     * @param methodHolders Methods for getting/loading/saving data
     */
    private StorageProvider(String name, Class<T> dataType, @NotNull Function<String, T> collector, @NotNull Function<StorageObject, T> loader, HashMap<String, MethodHolder<T, ?, ?>> methodHolders) {
        this.name = name;
        this.dataType = dataType;
        this.collector = collector;
        this.loader = loader;
        this.methodHolders = methodHolders;
    }

    public String getName() {
        return name;
    }

    public Class<T> getDataType() {
        return dataType;
    }

    @Nullable
    public T convertObject(StorageObject storageObject) {
        return loader.apply(storageObject);
    }

    public StorageObject convertObject(T obj) {
        StorageObject storageObject = new StorageObject(obj.getKey(), name);
        methodHolders.forEach((string, methodHolder) -> storageObject.set(string, methodHolder.getAndPrepareValue(obj), methodHolder.getConvertToLocalMethod()));
        return storageObject;
    }

    public CompletableFuture<StorageObject> getOrLoadObject(String uniqueId) {
        T data = collector.apply(uniqueId);
        if (data != null) {
            return CompletableFuture.completedFuture(convertObject(data));
        } else {
            return LushRewards.getInstance().getStorageManager().loadData(uniqueId, name);
        }
    }

    public MethodHolder<T, ?, ?> getMethodHolder(String id) {
        return methodHolders.get(id);
    }

    public HashMap<String, MethodHolder<T, ?, ?>> getMethodHolders() {
        return methodHolders;
    }

    public static class Builder<T extends Keyed> {
        private final String name;
        private final Class<T> dataType;
        private Function<String, T> collector;
        private Function<StorageObject, T> loader = null;
        private final HashMap<String, MethodHolder<T, ?, ?>> methodHolders = new HashMap<>();

        public Builder(String name, Class<T> dataType) {
            this.name = name;
            this.dataType = dataType;
        }

        public Builder<T> addString(String id, Function<T, String> getValueMethod) {
            methodHolders.put(id, new MethodHolder<>(getValueMethod, DataConstructor.Loadable.STRING, DataConstructor.Savable.STRING));
            return this;
        }

        public Builder<T> addInteger(String id, Function<T, Integer> getValueMethod) {
            methodHolders.put(id, new MethodHolder<>(getValueMethod, DataConstructor.Loadable.INTEGER, DataConstructor.Savable.INTEGER));
            return this;
        }

        public Builder<T> addBoolean(String id, Function<T, Boolean> getValueMethod) {
            methodHolders.put(id, new MethodHolder<>(getValueMethod, DataConstructor.Loadable.BOOLEAN, DataConstructor.Savable.BOOLEAN));
            return this;
        }

        public Builder<T> addDouble(String id, Function<T, Double> getValueMethod) {
            methodHolders.put(id, new MethodHolder<>(getValueMethod, DataConstructor.Loadable.DOUBLE, DataConstructor.Savable.DOUBLE));
            return this;
        }

        public Builder<T> addLong(String id, Function<T, Long> getValueMethod) {
            methodHolders.put(id, new MethodHolder<>(getValueMethod, DataConstructor.Loadable.LONG, DataConstructor.Savable.LONG));
            return this;
        }

        public <S> Builder<T> addObject(String id, Function<T, S> getValueMethod, Function<String, S> loadMethod, Function<S, String> saveMethod) {
            methodHolders.put(id, new MethodHolder<>(getValueMethod, loadMethod, saveMethod));
            return this;
        }

        public Builder<T> setLoader(Function<StorageObject, T> loader) {
            this.loader = loader;
            return this;
        }

        public Builder<T> setCollector(Function<String, T> collector) {
            this.collector = collector;
            return this;
        }

        public StorageProvider<T> build() {
            if (collector == null) {
                throw new NullPointerException("A collector method must be defined to build a storage provider");
            }
            if (loader == null) {
                throw new NullPointerException("A loader method must be defined to build a storage provider");
            }

            return new StorageProvider<>(name, dataType, collector, loader, methodHolders);
        }
    }

    /**
     * @param <T> Local Object Type
     * @param <L> Local Value Type
     * @param <S> Storage Type (The type to be stored in chosen data storage)
     */
    public static class MethodHolder<T, L, S> {
        private final Function<T, L> getValueMethod;
        private final Function<S, L> convertToLocalMethod;
        private final Function<L, S> convertToRemoteMethod;

        /**
         * @param getValueMethod Method to get local value
         * @param localTypeConverter Method to convert storage type to local type
         * @param storageTypeConverter Method to convert local type to storage type
         */
        public MethodHolder(Function<T, L> getValueMethod, Function<S, L> localTypeConverter, Function<L, S> storageTypeConverter) {
            this.getValueMethod = getValueMethod;
            this.convertToLocalMethod = localTypeConverter;
            this.convertToRemoteMethod = storageTypeConverter;
        }

        @Nullable
        public S getAndPrepareValue(T obj) {
            try {
                L localValue = getValueMethod.apply(obj);
                return convertToRemoteMethod.apply(localValue);
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
        }

        public Function<S, L> getConvertToLocalMethod() {
            return convertToLocalMethod;
        }
    }
}
