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
 * 净水系统的配方类型与序列化器注册。
 *
 * <p>注册三个烹饪配方类型（熔炉 / 高炉 / 烟熏炉），三者共用同一个序列化器，
 * 区别仅在于默认烹饪时间与所属设备。
 */
public final class PdopnRecipes {

    private static final String NAME_SMELTING = "purifying_smelting";
    private static final String NAME_BLASTING = "purifying_blasting";
    private static final String NAME_SMOKING = "purifying_smoking";

    /** 熔炉净化配方类型 */
    public static final RecipeType<PurifyingRecipe> PURIFYING_SMELTING = RecipeType.register(id(NAME_SMELTING).toString());
    /** 高炉净化配方类型 */
    public static final RecipeType<PurifyingRecipe> PURIFYING_BLASTING = RecipeType.register(id(NAME_BLASTING).toString());
    /** 烟熏炉净化配方类型 */
    public static final RecipeType<PurifyingRecipe> PURIFYING_SMOKING = RecipeType.register(id(NAME_SMOKING).toString());

    /** 三个设备共用的序列化器 */
    public static final RecipeSerializer<PurifyingRecipe> PURIFYING_SERIALIZER =
        RecipeSerializer.register(id("purifying").toString(), new Serializer());

    private PdopnRecipes() {}

    /** 由主类在 onInitialize 中调用（静态字段已完成注册，此处仅用于日志与显式触发类加载） */
    public static void register() {
        PerpetualDayOrPerpetualNight.LOGGER.info("[PDoPN] 已注册净水配方类型（熔炉 / 高炉 / 烟熏炉）");
    }

    private static Identifier id(String path) {
        return new Identifier(PerpetualDayOrPerpetualNight.MOD_ID, path);
    }

    /** 单个设备通道（配方类型 + 默认烹饪时间） */
    private record Channel(RecipeType<PurifyingRecipe> type, int defaultCookTime) {}

    private static Channel channelFor(String type) {
        if (type.equals(id(NAME_SMELTING).toString())) return new Channel(PURIFYING_SMELTING, 200);
        if (type.equals(id(NAME_BLASTING).toString())) return new Channel(PURIFYING_BLASTING, 100);
        if (type.equals(id(NAME_SMOKING).toString())) return new Channel(PURIFYING_SMOKING, 100);
        return null;
    }

    /**
     * 净水配方序列化器。
     *
     * <p>关键校验：{@code ingredient} 解析后，其所有匹配项都必须是
     * 「水瓶」或「水桶」，否则拒绝加载该配方。
     * 这样才能真正阻止「任意药水被烧成净水瓶」。
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

            return create(channel, id, group, category, input, output, experience, cookTime);
        }

        @Override
        public PurifyingRecipe read(Identifier id, PacketByteBuf buf) {
            String group = buf.readString();
            CookingRecipeCategory category = buf.readEnumConstant(CookingRecipeCategory.class);
            Ingredient input = Ingredient.fromPacket(buf);
            ItemStack output = buf.readItemStack();
            float experience = buf.readFloat();
            int cookTime = buf.readVarInt();
            // 客户端按 recipe id 归类，type 传熔炉通道即可满足 Recipe#getType 的非空约定
            return create(channelFor(id(NAME_SMELTING).toString()), id, group, category,
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

        private static PurifyingRecipe create(Channel channel, Identifier id, String group,
                                             CookingRecipeCategory category, Ingredient input,
                                             ItemStack output, float experience, int cookTime) {
            if (channel.type() == PURIFYING_BLASTING) {
                return new PurifyingRecipe.Blasting(id, group, category, input, output, experience, cookTime);
            }
            if (channel.type() == PURIFYING_SMOKING) {
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
