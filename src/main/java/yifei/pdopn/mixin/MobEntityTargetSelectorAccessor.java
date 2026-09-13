package yifei.pdopn.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 {@link MobEntity#targetSelector}（protected 字段）。
 *
 * <p>取代此前的 {@code getDeclaredField("targetSelector") + setAccessible} 反射方案：
 * 反射字段名在混淆环境下脆弱、无编译期校验、且每次访问都有额外开销。
 * Accessor Mixin 由 Mixin 在编译期完成映射，名字与类型都受编译器检查。
 *
 * <p>选择独立 accessor 接口而不是把逻辑直接塞进 {@link MobEntity} 的 Mixin，
 * 是为了让「AI 目标注入」这项改动保持最小侵入，且不额外影响任何原版行为。
 */
@Mixin(MobEntity.class)
public interface MobEntityTargetSelectorAccessor {

    /** 只读访问目标选择器（调用方只做 add / clear，不需要替换实例） */
    @Accessor("targetSelector")
    GoalSelector pdopn$getTargetSelector();
}
