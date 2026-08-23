package yifei.pdopn.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yifei.pdopn.mode.PdopnMode;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 敌对生物属性与 AI 修改器。
 * 职责：模式切换时对敌对生物施加属性修正和 AI 增强，恢复时还原。
 *
 * <p>判定优先级：Boss > 亡灵排除（仅永昼） > 普通敌对。
 * 亡灵生物在永昼模式下不处理（持续日照燃烧），永夜模式下正常处理。
 */
public final class PdopnEntityModifier {

    private static final Logger LOGGER = LoggerFactory.getLogger("PDoPN-EntityModifier");

    /* ────────── 固定 UUID，用于增删属性修正 ────────── */

    /** 永昼模式速度修正 */
    private static final UUID PD_SPEED_UUID = UUID.fromString("a0e1c2d3-1111-4000-8000-000000000001");
    /** 永昼模式攻击修正 */
    private static final UUID PD_ATTACK_UUID = UUID.fromString("a0e1c2d3-1111-4000-8000-000000000002");
    /** 永夜模式速度修正 */
    private static final UUID PN_SPEED_UUID = UUID.fromString("a0e1c2d3-2222-4000-8000-000000000001");
    /** 永夜模式攻击修正 */
    private static final UUID PN_ATTACK_UUID = UUID.fromString("a0e1c2d3-2222-4000-8000-000000000002");

    /** AI 目标注入优先级（高于大多数原生目标选择） */
    private static final int PLAYER_TARGET_PRIORITY = 0;

    /** MobEntity.targetSelector 反射字段（protected，需反射访问） */
    private static final Field TARGET_SELECTOR_FIELD;
    static {
        Field f;
        try {
            f = net.minecraft.entity.mob.MobEntity.class.getDeclaredField("targetSelector");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            f = null;
            LOGGER.error("无法反射获取 MobEntity.targetSelector");
        }
        TARGET_SELECTOR_FIELD = f;
    }

    /** 已修改实体 → 原始属性快照 */
    private final Map<UUID, OriginalAttributes> modifiedEntities = new HashMap<>();
    /** 已注入 AI 目标的实体 UUID 集合 */
    private final Set<UUID> aiModifiedEntities = new HashSet<>();

    /* ══════════ 公开 API ══════════ */

    /** 模式切换时调用，对当前所有世界的生物施加或还原修改 */
    public void onModeChanged(PdopnMode mode, MinecraftServer server) {
        if (mode == PdopnMode.NORMAL) {
            removeAllModifications(server);
        } else {
            // 先清除旧修改，再施加新模式
            removeAllModifications(server);
            applyModifications(mode, server);
        }
    }

    /** 实体加载（生成/区块加载）时调用，按当前模式施加修改 */
    public void onEntityLoaded(Entity entity, PdopnMode currentMode) {
        if (currentMode == PdopnMode.NORMAL || !(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;
        if (!isEligibleHostile(living)) {
            return;
        }
        applyToEntity(living, currentMode);
    }

    /* ══════════ 实体分类 ══════════ */

    /** 是否为敌对生物（实现 Monster 接口） */
    private boolean isEligibleHostile(LivingEntity entity) {
        return entity instanceof Monster;
    }

    /** 是否为 Boss 生物（末影龙、凋灵） — Boss 优先级高于亡灵排除 */
    private boolean isBoss(LivingEntity entity) {
        return entity instanceof EnderDragonEntity || entity instanceof WitherEntity;
    }

    /** 是否为亡灵生物 */
    private boolean isUndead(LivingEntity entity) {
        return entity.getGroup() == EntityGroup.UNDEAD;
    }

    /**
     * 是否为中立敌对生物（同时实现 Monster 和 Angerable）。
     * 这类生物默认不主动追踪玩家，需注入 AI 目标。
     * 包括：末影人、蜘蛛、洞穴蜘蛛、僵尸猪灵等。
     */
    private boolean isNeutralHostile(LivingEntity entity) {
        return entity instanceof Monster && entity instanceof Angerable;
    }

    /**
     * 判断实体在当前模式下是否应被修改。
     * 永昼：排除亡灵（燃烧），Boss 始终处理。
     * 永夜：所有敌对生物均处理。
     */
    private boolean shouldBeModified(LivingEntity entity, PdopnMode mode) {
        if (isBoss(entity)) return true;
        if (mode == PdopnMode.PERPETUAL_DAY && isUndead(entity)) return false;
        return true;
    }

    /* ══════════ 批量操作 ══════════ */

    /** 遍历所有世界，对符合条件的实体施加修改 */
    private void applyModifications(PdopnMode mode, MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (!(entity instanceof LivingEntity)) continue;
                LivingEntity living = (LivingEntity) entity;
                if (!isEligibleHostile(living) || !shouldBeModified(living, mode)) continue;
                applyToEntity(living, mode);
            }
        }
    }

    /** 还原所有已修改实体的属性和 AI */
    private void removeAllModifications(MinecraftServer server) {
        if (modifiedEntities.isEmpty()) return;

        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (!(entity instanceof LivingEntity)) continue;
                OriginalAttributes orig = modifiedEntities.get(entity.getUuid());
                if (orig != null) {
                    restoreEntity((LivingEntity) entity, orig);
                }
            }
        }
        modifiedEntities.clear();
        aiModifiedEntities.clear();
    }

    /* ══════════ 属性修改 ══════════ */

    /** 对单个实体施加模式对应的属性修改和 AI 增强 */
    private void applyToEntity(LivingEntity entity, PdopnMode mode) {
        // 避免重复修改
        if (modifiedEntities.containsKey(entity.getUuid())) return;

        double originalMaxHp = entity.getMaxHealth();
        double originalCurrentHp = entity.getHealth();
        double originalBaseHp = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getBaseValue();

        // 保存原始快照
        OriginalAttributes orig = new OriginalAttributes(originalBaseHp, originalMaxHp, originalCurrentHp);

        if (isBoss(entity)) {
            applyBossAttributes(entity, mode);
        } else {
            applyRegularAttributes(entity, mode);
        }

        // 仅为中立敌对生物注入玩家追踪 AI（已自带玩家追踪的不重复注入）
        if (isNeutralHostile(entity)) {
            addPlayerTargetGoal(entity);
            aiModifiedEntities.add(entity.getUuid());
        }

        modifiedEntities.put(entity.getUuid(), orig);
    }

    /** 普通敌对生物属性修改 */
    private void applyRegularAttributes(LivingEntity entity, PdopnMode mode) {
        EntityAttributeInstance healthAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr == null) return;

        double originalMaxHp = entity.getMaxHealth();
        double targetHp;
        UUID speedUuid;
        double speedMultiplier;
        UUID attackUuid;
        double attackMultiplier;

        if (mode == PdopnMode.PERPETUAL_DAY) {
            targetHp = 200.0;
            speedUuid = PD_SPEED_UUID;
            // MULTIPLY_BASE 0.5 → 最终值 = base × (1 + 0.5) = base × 1.5
            speedMultiplier = 0.5;
            attackUuid = PD_ATTACK_UUID;
            // MULTIPLY_BASE -0.5 → 最终值 = base × (1 + (-0.5)) = base × 0.5
            attackMultiplier = -0.5;
        } else {
            targetHp = 1000.0;
            speedUuid = PN_SPEED_UUID;
            // MULTIPLY_BASE -0.3 → 最终值 = base × 0.7
            speedMultiplier = -0.3;
            attackUuid = PN_ATTACK_UUID;
            // MULTIPLY_BASE 4.0 → 最终值 = base × 5.0
            attackMultiplier = 4.0;
        }

        // 设置血量（setBaseValue 不受钳制，computeValue 时才钳制到 1024）
        healthAttr.setBaseValue(targetHp);
        scaleHealth(entity, originalMaxHp);

        // 移速修正
        addModifierIfPresent(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED,
            new EntityAttributeModifier(speedUuid, "pdopn_speed", speedMultiplier,
                EntityAttributeModifier.Operation.MULTIPLY_BASE));

        // 攻击修正
        addModifierIfPresent(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE,
            new EntityAttributeModifier(attackUuid, "pdopn_attack", attackMultiplier,
                EntityAttributeModifier.Operation.MULTIPLY_BASE));
    }

    /** Boss 生物属性修改 */
    private void applyBossAttributes(LivingEntity entity, PdopnMode mode) {
        EntityAttributeInstance healthAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr == null) return;

        double originalMaxHp = entity.getMaxHealth();
        double hpMultiplier;
        double attackMultiplier;
        UUID speedUuid;
        double speedMultiplier;
        UUID attackUuid;

        if (mode == PdopnMode.PERPETUAL_DAY) {
            hpMultiplier = 2.5;
            attackMultiplier = -0.3;   // × 0.7
            speedUuid = PD_SPEED_UUID;
            speedMultiplier = 0.5;     // × 1.5
            attackUuid = PD_ATTACK_UUID;
        } else {
            hpMultiplier = 10.0;
            attackMultiplier = 2.0;    // × 3.0
            speedUuid = PN_SPEED_UUID;
            speedMultiplier = -0.3;    // × 0.7
            attackUuid = PN_ATTACK_UUID;
        }

        // 设置血量（结果会被 ClampedEntityAttribute 钳制到 ≤1024）
        double targetHp = originalMaxHp * hpMultiplier;
        healthAttr.setBaseValue(targetHp);
        scaleHealth(entity, originalMaxHp);

        // 移速修正（同时处理地面移速和飞行移速）
        addModifierIfPresent(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED,
            new EntityAttributeModifier(speedUuid, "pdopn_speed", speedMultiplier,
                EntityAttributeModifier.Operation.MULTIPLY_BASE));
        addModifierIfPresent(entity, EntityAttributes.GENERIC_FLYING_SPEED,
            new EntityAttributeModifier(speedUuid, "pdopn_fly_speed", speedMultiplier,
                EntityAttributeModifier.Operation.MULTIPLY_BASE));

        // 攻击修正
        addModifierIfPresent(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE,
            new EntityAttributeModifier(attackUuid, "pdopn_attack", attackMultiplier,
                EntityAttributeModifier.Operation.MULTIPLY_BASE));
    }

    /* ══════════ 属性还原 ══════════ */

    /** 还原单个实体的属性和 AI */
    private void restoreEntity(LivingEntity entity, OriginalAttributes orig) {
        // 还原血量基础值
        EntityAttributeInstance healthAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(orig.originalBaseHp);
            // 按原始最大 HP 的比例还原当前 HP
            double newMaxHp = entity.getMaxHealth();
            if (orig.originalMaxHp > 0) {
                entity.setHealth((float) (entity.getHealth() / newMaxHp * orig.originalMaxHp));
            }
        }

        // 移除所有模式的属性修正
        removeModifier(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, PD_SPEED_UUID);
        removeModifier(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, PN_SPEED_UUID);
        removeModifier(entity, EntityAttributes.GENERIC_FLYING_SPEED, PD_SPEED_UUID);
        removeModifier(entity, EntityAttributes.GENERIC_FLYING_SPEED, PN_SPEED_UUID);
        removeModifier(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, PD_ATTACK_UUID);
        removeModifier(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, PN_ATTACK_UUID);

        // 移除注入的 AI 目标
        removePlayerTargetGoal(entity);
    }

    /* ══════════ AI 修改 ══════════ */

    /** 通过反射获取实体的 targetSelector */
    private net.minecraft.entity.ai.goal.GoalSelector getTargetSelector(LivingEntity entity) {
        if (!(entity instanceof MobEntity) || TARGET_SELECTOR_FIELD == null) return null;
        try {
            return (net.minecraft.entity.ai.goal.GoalSelector) TARGET_SELECTOR_FIELD.get(entity);
        } catch (IllegalAccessException e) {
            LOGGER.error("反射访问 targetSelector 失败: {}", e.getMessage());
            return null;
        }
    }

    /** 注入高优先级玩家追踪目标，使中立敌对生物主动搜寻并攻击玩家 */
    private void addPlayerTargetGoal(LivingEntity entity) {
        net.minecraft.entity.ai.goal.GoalSelector selector = getTargetSelector(entity);
        if (selector == null) return;
        MobEntity mob = (MobEntity) entity;
        selector.add(PLAYER_TARGET_PRIORITY,
            new ActiveTargetGoal<>(mob, PlayerEntity.class, true));
    }

    /**
     * 移除注入的玩家追踪目标。
     * 仅对 aiModifiedEntities 中记录的中立敌对生物执行，
     * 移除其所有 ActiveTargetGoal（这些生物原生不使用此类目标）。
     */
    private void removePlayerTargetGoal(LivingEntity entity) {
        if (!aiModifiedEntities.contains(entity.getUuid())) return;
        net.minecraft.entity.ai.goal.GoalSelector selector = getTargetSelector(entity);
        if (selector == null) return;
        selector.clear(goal -> goal instanceof ActiveTargetGoal);
    }

    /* ══════════ 工具方法 ══════════ */

    /** 按原始最大 HP 比例缩放当前 HP */
    private void scaleHealth(LivingEntity entity, double oldMaxHp) {
        double newMaxHp = entity.getMaxHealth();
        if (oldMaxHp > 0) {
            entity.setHealth((float) (entity.getHealth() / oldMaxHp * newMaxHp));
        }
    }

    /** 仅当实体拥有该属性时添加修正 */
    private void addModifierIfPresent(LivingEntity entity,
                                      net.minecraft.entity.attribute.EntityAttribute attribute,
                                      EntityAttributeModifier modifier) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.addTemporaryModifier(modifier);
        }
    }

    /** 移除指定属性上的指定 UUID 修正 */
    private void removeModifier(LivingEntity entity,
                                net.minecraft.entity.attribute.EntityAttribute attribute,
                                UUID uuid) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }

    /** 原始属性快照，用于模式恢复时还原 */
    private static final class OriginalAttributes {
        final double originalBaseHp;
        final double originalMaxHp;
        final double originalCurrentHp;

        OriginalAttributes(double baseHp, double maxHp, double currentHp) {
            this.originalBaseHp = baseHp;
            this.originalMaxHp = maxHp;
            this.originalCurrentHp = currentHp;
        }
    }
}
