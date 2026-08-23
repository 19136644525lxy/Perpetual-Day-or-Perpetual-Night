/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.loot;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.lang.invoke.TypeDescriptor;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.loot.LootDataKey;
import net.minecraft.loot.LootGsons;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContextAware;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

public class LootDataType<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final LootDataType<LootCondition> PREDICATES = new LootDataType(LootGsons.getConditionGsonBuilder().create(), LootDataType.parserFactory(LootCondition.class, LootManager::and), "predicates", LootDataType.validator());
    public static final LootDataType<LootFunction> ITEM_MODIFIERS = new LootDataType(LootGsons.getFunctionGsonBuilder().create(), LootDataType.parserFactory(LootFunction.class, LootManager::and), "item_modifiers", LootDataType.validator());
    public static final LootDataType<LootTable> LOOT_TABLES = new LootDataType<LootTable>(LootGsons.getTableGsonBuilder().create(), LootDataType.parserFactory(LootTable.class), "loot_tables", LootDataType.tableValidator());
    private final Gson gson;
    private final BiFunction<Identifier, JsonElement, Optional<T>> parser;
    private final String id;
    private final Validator<T> validator;

    private LootDataType(Gson gson, BiFunction<Gson, String, BiFunction<Identifier, JsonElement, Optional<T>>> parserFactory, String id, Validator<T> validator) {
        this.gson = gson;
        this.id = id;
        this.validator = validator;
        this.parser = parserFactory.apply(gson, id);
    }

    public Gson getGson() {
        return this.gson;
    }

    public String getId() {
        return this.id;
    }

    public void validate(LootTableReporter reporter, LootDataKey<T> key, T value) {
        this.validator.run(reporter, key, value);
    }

    public Optional<T> parse(Identifier id, JsonElement json) {
        return this.parser.apply(id, json);
    }

    public static Stream<LootDataType<?>> stream() {
        return Stream.of(PREDICATES, ITEM_MODIFIERS, LOOT_TABLES);
    }

    private static <T> BiFunction<Gson, String, BiFunction<Identifier, JsonElement, Optional<T>>> parserFactory(Class<T> clazz) {
        return (gson, dataTypeId) -> (id, json) -> {
            try {
                return Optional.of(gson.fromJson((JsonElement)json, clazz));
            } catch (Exception exception) {
                LOGGER.error("Couldn't parse element {}:{}", dataTypeId, id, exception);
                return Optional.empty();
            }
        };
    }

    private static <T> BiFunction<Gson, String, BiFunction<Identifier, JsonElement, Optional<T>>> parserFactory(Class<T> clazz, Function<T[], T> combiner) {
        TypeDescriptor.OfField class_ = clazz.arrayType();
        return (arg_0, arg_1) -> LootDataType.method_51213((Class)class_, combiner, clazz, arg_0, arg_1);
    }

    private static <T extends LootContextAware> Validator<T> validator() {
        return (reporter, key, value) -> value.validate(reporter.makeChild("{" + key.type().id + ":" + key.id() + "}", key));
    }

    private static Validator<LootTable> tableValidator() {
        return (reporter, key, value) -> value.validate(reporter.withContextType(value.getType()).makeChild("{" + key.type().id + ":" + key.id() + "}", key));
    }

    private static /* synthetic */ BiFunction method_51213(Class class_, Function function, Class class2, Gson gson, String dataTypeId) {
        return (id, json) -> {
            try {
                if (json.isJsonArray()) {
                    Object[] objects = (Object[])gson.fromJson((JsonElement)json, class_);
                    return Optional.of(function.apply(objects));
                }
                return Optional.of(gson.fromJson((JsonElement)json, class2));
            } catch (Exception exception) {
                LOGGER.error("Couldn't parse element {}:{}", dataTypeId, id, exception);
                return Optional.empty();
            }
        };
    }

    @FunctionalInterface
    public static interface Validator<T> {
        public void run(LootTableReporter var1, LootDataKey<T> var2, T var3);
    }
}

