package yifei.pdopn.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import yifei.pdopn.PerpetualDayOrPerpetualNight;
import yifei.pdopn.thirst.ThirstData;

/**
 * 净水配方的序列化器注册。
 *
 * <p><b>只注册序列化器，不注册自定义 RecipeType</b>：炉子按
 * {@code RecipeType.SMELTING / BLASTING / SMOKING} 建立索引并查询
 * （见 {@code RecipeManager#getAllOfType}），配方若报告自定义类型就会被放进
 * 永远不会被查询的桶里，表现为「配方存在但炉子不认」。
 * 因此 {@link PurifyingRecipe} 直接报告设备对应的原版配方类型。
 *
 * <p>真正的输入保护在本类的 {@link Serializer}：反序列化阶段即拒绝任何
 * 非「水瓶 / 水桶」的输入，从而解决 1.20.1 {@code Ingredient} 无法表达 NBT 条件、
 * 导致任意药水都能被烧成净水瓶的问题。
 */
public final class PdopnRecipes {

    private static final String NAME_SMELTING = "purifying_smelting";
    private static final String NAME_BLASTING = "purifying_blasting";
    private static final String NAME_SMOKING = "purifying_smoking";

    /** 三个设备共用的序列化器 */
    public static final RecipeSerializer<PurifyingRecipe> PURIFYING_SERIALIZER =
        RecipeSerializer.register(id("purifying").toString(), new Serializer());

    private PdopnRecipes() {}

    /** 由主类在 onInitialize 中调用（静态字段已完成注册，此处仅用于日志与显式触发类加载） */
    public static void register() {
        PerpetualDayOrPerpetualNight.LOGGER.info("[PDoPN] 已注册净水配方序列化器（熔炉 / 高炉 / 烟熏炉）");
    }

    private static Identifier id(String path) {
        return new Identifier(PerpetualDayOrPerpetualNight.MOD_ID, path);
    }

    /** 单个设备通道（默认烹饪时间 + 产物构造方式） */
    private record Channel(int defaultCookTime) {}

    private static Channel channelFor(String type) {
        if (type.equals(id(NAME_SMELTING).toString())) return new Channel(200);
        if (type.equals(id(NAME_BLASTING).toString())) return new Channel(100);
        if (type.equals(id(NAME_SMOKING).toString())) return new Channel(100);
        return null;
    }

    /**
     * 净水配方序列化器。
     *
     * <p>关键校验：{@code ingredient} 解析后，其所有匹配项都必须是
     * 「水瓶」或「水桶」，否则拒绝加载该配方。
     */
    private static final class Serializer implements RecipeSerializer<PurifyingRecipe> {

        @Override
        public PurifyingRecipe read(Identifier id, JsonObject json) {
            String type = JsonHelper.getString(json, "type");
            Channel channel = channelFor(type);
            if (channel == null) {
                // 只允许抛 JsonParseException / IllegalArgumentException，避免影响其他配方加载
                throw new IllegalArgumentException("Unknown purifying recipe type: " + type);
            }

            String group = JsonHelper.getString(json, "group", "");
            CookingRecipeCategory category = CookingRecipeCategory.CODEC
                .byId(JsonHelper.getString(json, "category", null), CookingRecipeCategory.MISC);

            // 解析并严格校验输入白名单
            JsonElement declared = JsonHelper.hasArray(json, "ingredient")
                ? JsonHelper.getArray(json, "ingredient")
                : JsonHelper.getObject(json, "ingredient");
            Ingredient input = Ingredient.fromJson(declared, false);
            ensureAllowedOnly(id, input);

            String resultId = JsonHelper.getString(json, "result");
            ItemStack output = new ItemStack(
                Registries.ITEM.getOrEmpty(new Identifier(resultId))
                    .orElseThrow(() -> new IllegalArgumentException("Item: " + resultId + " does not exist"))
            );

            float experience = JsonHelper.getFloat(json, "experience", 0.0F);
            int cookTime = JsonHelper.getInt(json, "cookingtime", channel.defaultCookTime());

            return create(type, id, group, category, input, output, experience, cookTime);
        }

        @Override
        public PurifyingRecipe read(Identifier id, PacketByteBuf buf) {
            // 客户端从同步过来的配方只拿到 id，无法得知设备类型；
            // 客户端不参与炉子匹配（匹配在服务端进行），这里取熔炉变体即可，
            // 其 getType() 为 RecipeType.SMELTING，可正常进入配方表。
            String group = buf.readString();
            CookingRecipeCategory category = buf.readEnumConstant(CookingRecipeCategory.class);
            Ingredient input = Ingredient.fromPacket(buf);
            ItemStack output = buf.readItemStack();
            float experience = buf.readFloat();
            int cookTime = buf.readVarInt();
            return create(id(NAME_SMELTING).toString(), id, group, category,
                input, output, experience, cookTime);
        }

        @Override
        public void write(PacketByteBuf buf, PurifyingRecipe recipe) {
            buf.writeString(recipe.getGroup());
            buf.writeEnumConstant(recipe.getCategory());
            recipe.getIngredients().get(0).write(buf);
            buf.writeItemStack(recipe.getOutput(null));
            buf.writeFloat(recipe.getExperience());
            buf.writeVarInt(recipe.getCookTime());
        }

        private static PurifyingRecipe create(String type, Identifier id, String group,
                                             CookingRecipeCategory category, Ingredient input,
                                             ItemStack output, float experience, int cookTime) {
            if (type.equals(id(NAME_BLASTING).toString())) {
                return new PurifyingRecipe.Blasting(id, group, category, input, output, experience, cookTime);
            }
            if (type.equals(id(NAME_SMOKING).toString())) {
                return new PurifyingRecipe.Smoking(id, group, category, input, output, experience, cookTime);
            }
            return new PurifyingRecipe.Smelting(id, group, category, input, output, experience, cookTime);
        }

        /** 输入必须全部落在白名单内（水瓶 / 水桶） */
        private static void ensureAllowedOnly(Identifier id, Ingredient ingredient) {
            if (ingredient.isEmpty()) {
                throw new IllegalArgumentException("Purifying recipe " + id + " has an empty ingredient");
            }
            for (ItemStack stack : ingredient.getMatchingStacks()) {
                if (!ThirstData.isAllowedPurifyingInput(stack)) {
                    throw new IllegalArgumentException(
                        "Purifying recipe " + id + " may only accept a water bottle or water bucket, but got: "
                            + stack.getItem());
                }
            }
        }
    }
}
