package ru.florestdev.dvizhukha_forge;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

public class KhokholKnight extends Item {

    public KhokholKnight(Properties properties) {
        super(properties
                .fireResistant()
                .rarity(Rarity.EPIC)
                .durability(15)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED,
                                new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build()
                )
        );
    }

    @Override
    public Component getName(ItemStack stack) {
        String owner = getOwnerName(stack);
        return Component.literal("Нож " + owner)
                .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            if (!player.level().isClientSide()) {
                target.hurt(
                        target.damageSources().playerAttack(player),
                        5.0F
                );
            }

            if (player instanceof ServerPlayer serverPlayer) {
                stack.hurtAndBreak(
                        1,
                        serverPlayer.level(),
                        serverPlayer,
                        item -> {}
                );
            }
        }
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    private String getOwnerName(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            if (name != null) {
                return name.getString();
            }
        }
        return "Аноним";
    }

    public void setOwnerName(ItemStack stack, String name) {
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(name)
        );
        stack.set(
                DataComponents.LORE,
                new ItemLore(
                        java.util.List.of(
                                Component.literal("Принадлежит " + name)
                                        .withStyle(ChatFormatting.GRAY)
                        )
                )
        );
    }

    public ItemStack createWithOwner(String ownerName) {
        ItemStack stack = new ItemStack(this);
        setOwnerName(stack, ownerName);
        return stack;
    }

    public void giveToPlayer(ServerPlayer player, String ownerName) {
        ItemStack stack = createWithOwner(ownerName);
        player.getInventory().add(stack);
        if (!player.getInventory().contains(stack)) {
            player.drop(stack, false);
        }
    }

    public boolean isOwnedBy(ItemStack stack, String playerName) {
        String owner = getOwnerName(stack);
        return owner.equals(playerName);
    }

    public void removeOwner(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_NAME);
        stack.remove(DataComponents.LORE);
    }
}