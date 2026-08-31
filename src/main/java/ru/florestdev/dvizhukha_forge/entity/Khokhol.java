package ru.florestdev.dvizhukha_forge.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;

public class Khokhol extends Pig {
    private float scale = 1.5f;
    private int attackCooldown = 0;

    public Khokhol(EntityType<? extends Pig> entityType, Level world) {
        super(entityType, world);
        this.setCustomName(net.minecraft.network.chat.Component.literal("§c§lХохол"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true));

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        // не даем делаться этой хрени в пиглина.
    }

    @Override
    public void tick() {
        super.tick();
        if (attackCooldown > 0) attackCooldown--;

        if (!this.level().isClientSide()) {
            if (this.getTarget() == null || !this.getTarget().isAlive()) {
                Player nearestPlayer = this.level().getNearestPlayer(this, 20.0);
                if (nearestPlayer != null) {
                    this.setTarget(nearestPlayer);
                }
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double distance = this.distanceToSqr(target);
                if (distance < 4.0 && attackCooldown == 0) {
                    attackCooldown = 10;

                    float damage = 6.0f;
                    var damageAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (damageAttr != null) {
                        damage = (float) damageAttr.getValue();
                    }

                    if (this.level() instanceof ServerLevel serverLevel) {
                        target.hurt(
                                this.damageSources().mobAttack(this),
                                damage
                        );
                    }

                    target.knockback(0.5, target.getX() - this.getX(), target.getZ() - this.getZ());
                    this.playSound(SoundEvents.PIG_AMBIENT, 1.0F, 1.0F);
                }
            }
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (damageSource.getEntity() instanceof ServerPlayer player) {
            var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            var attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);

            if (maxHealth != null) {
                maxHealth.addPermanentModifier(
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("dvizhukha_forge", "khokhol_health"),
                                6.0,
                                AttributeModifier.Operation.ADD_VALUE
                        )
                );
                player.setHealth(player.getMaxHealth());
            }

            if (attackDamage != null) {
                attackDamage.addPermanentModifier(
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("dvizhukha_forge", "khokhol_damage"),
                                2.0,
                                AttributeModifier.Operation.ADD_VALUE
                        )
                );
            }

            if (player.level() instanceof ServerLevel world) {
                MinecraftServer server = world.getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§c§lХОХОЛ ПОВЕРЖЕН! §f"
                                            + player.getName().getString()
                                            + " §7получил §c+3 сердца §7и §4+2 урона§7!"
                            ),
                            false
                    );
                }
            }
        }
        super.die(damageSource);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            this.setTarget(attacker);
        }
        return super.hurtServer(level, source, amount);
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.5f, Math.min(5.0f, scale));
        this.refreshDimensions();
    }


    public static AttributeSupplier.Builder createKhokholAttributes() {
        return Pig.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ATTACK_SPEED, 1.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }
}