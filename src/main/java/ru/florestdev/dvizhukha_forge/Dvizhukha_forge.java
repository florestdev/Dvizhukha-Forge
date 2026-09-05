package ru.florestdev.dvizhukha_forge;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import ru.florestdev.dvizhukha_forge.entity.Khokhol;
import ru.florestdev.dvizhukha_forge.KhokholKnight;

import java.util.List;
import java.util.Random;

@Mod(Dvizhukha_forge.MOD_ID)
public class Dvizhukha_forge {
    public static final String MOD_ID = "dvizhukha_forge";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    // Регистрация сущностей
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

    // Регистрация предметов
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, MOD_ID);

    // Регистрация сущности Khokhol
    public static final DeferredHolder<EntityType<?>, EntityType<Khokhol>> KHOKHOL =
            ENTITY_TYPES.register(
                    "khokhol",
                    () -> EntityType.Builder.of(Khokhol::new, MobCategory.CREATURE)
                            .sized(1.5f, 1.5f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(MOD_ID, "khokhol")))
            );

    // Регистрация меча KhokholKnight
    // Регистрация меча KhokholKnight
    public static final DeferredHolder<Item, KhokholKnight> KHOKHOL_KNIGHT =
            ITEMS.register(
                    "khokhol_knight",
                    () -> new KhokholKnight(
                            new Item.Properties().setId(
                                    ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "khokhol_knight"))
                            )
                    )
            );

    private int tickCounter = 0;
    private static final int SPAWN_INTERVAL = 6000;

    // Статический геттер для доступа к мечу из других классов
    public static KhokholKnight getKhokholKnight() {
        return KHOKHOL_KNIGHT.get();
    }

    public Dvizhukha_forge(IEventBus modEventBus) {
        LOGGER.info("Dvizhukha mod initialized!");

        // Регистрируем все DeferredRegister
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerAttributes);

        // Регистрируем клиентский setup
        modEventBus.addListener(ClientSetup::onClientSetup);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Dvizhukha common setup completed!");

        // Дополнительная инициализация при необходимости
        event.enqueueWork(() -> {
            LOGGER.info("KhokholKnight initialized with ID: {}",
                    KHOKHOL_KNIGHT.getId().toString());
        });
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(KHOKHOL.get(), Khokhol.createKhokholAttributes().build());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= SPAWN_INTERVAL) {
            tickCounter = 0;
            pigCycle(event.getServer());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        // Команда /khokhol - спавн хохла
        dispatcher.register(
                net.minecraft.commands.Commands.literal("khokhol")
                        .executes(context -> {
                            var source = context.getSource();

                            if (source.getEntity() == null) {
                                pigCycle(source.getServer());
                                source.sendSuccess(
                                        () -> net.minecraft.network.chat.Component.literal(
                                                "§aЦикл появления хохлов успешно запущен."),
                                        true
                                );
                                return 1;
                            }

                            if (source.getEntity() instanceof ServerPlayer player) {
                                var server = source.getServer();

                                if (!server.isDedicatedServer()) {
                                    pigCycle(server);
                                    source.sendSuccess(
                                            () -> net.minecraft.network.chat.Component.literal(
                                                    "§aЦикл появления хохлов успешно запущен."),
                                            true
                                    );
                                    return 1;
                                }

                                if (!server.getPlayerList().isOp(new NameAndId(player.getUUID(), player.getName().getString()))) {
                                    source.sendFailure(
                                            net.minecraft.network.chat.Component.literal(
                                                    "§cУ тебя нет прав на эту команду.")
                                    );
                                    return 0;
                                }

                                pigCycle(server);
                                source.sendSuccess(
                                        () -> net.minecraft.network.chat.Component.literal(
                                                "§aЦикл появления хохлов успешно запущен."),
                                        true
                                );
                                return 1;
                            }

                            return 0;
                        })
        );
    }

    private void pigCycle(MinecraftServer server) {
        LOGGER.info("Так, спавним хохла...");

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        ServerPlayer player = players.get(RANDOM.nextInt(players.size()));
        if (player == null || !player.isAlive()) return;

        Level world = player.level();
        var playerPos = player.blockPosition();

        // Спавн молнии
        var lightning = EntityType.LIGHTNING_BOLT.create(world, EntitySpawnReason.COMMAND);
        if (lightning != null) {
            lightning.setPos(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
            world.addFreshEntity(lightning);
        }

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§c⚡ Молния ударила рядом с вами!")
        );

        // Асинхронный спавн хохла с задержкой
        server.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            server.execute(() -> {
                var spawnPos = playerPos.offset(
                        RANDOM.nextInt(5) - 2,
                        0,
                        RANDOM.nextInt(5) - 2
                );

                if (!world.getBlockState(spawnPos).isAir()) {
                    spawnPos = playerPos.above(2);
                }

                Khokhol khokhol = KHOKHOL.get().create(world, EntitySpawnReason.COMMAND);
                if (khokhol != null) {
                    khokhol.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    khokhol.setScale(1.5f);
                    khokhol.setPersistenceRequired();
                    khokhol.setTarget(player);

                    world.addFreshEntity(khokhol);

                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§cИз молнии появился ВСУшник-хохол! ЗАМОЧИ ЕГО")
                    );
                    LOGGER.info("Хохол заспавнен!");
                }
            });
        });
    }
}