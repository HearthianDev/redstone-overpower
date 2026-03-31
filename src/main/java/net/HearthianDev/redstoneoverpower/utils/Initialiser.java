package net.HearthianDev.redstoneoverpower.utils;

import net.HearthianDev.redstoneoverpower.RedstoneOverpower;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.HearthianDev.redstoneoverpower.block.*;
import net.HearthianDev.redstoneoverpower.block.entity.DuctBlockEntity;
import net.HearthianDev.redstoneoverpower.block.entity.LogicalComparatorBlockEntity;
import net.HearthianDev.redstoneoverpower.block.entity.SculkChamberBlockEntity;
import net.HearthianDev.redstoneoverpower.block.screen.DuctScreenHandler;


public class Initialiser {
  public static final LogicalComparatorBlock LOGICAL_COMPARATOR_BLOCK = new LogicalComparatorBlock(
          BlockBehaviour.Properties.ofFullCopy(Blocks.COMPARATOR).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "logical_comparator")))
  );
  public static final SculkChamberBlock SCULK_CHAMBER_BLOCK = new SculkChamberBlock(
          BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "sculk_chamber")))
  );
  public static final SculkNoteBlock SCULK_NOTE_BLOCK = new SculkNoteBlock(
          BlockBehaviour.Properties.ofFullCopy(Blocks.NOTE_BLOCK).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "sculk_note_block")))
  );
  public static final SculkPulserBlock SCULK_PULSER_BLOCK = new SculkPulserBlock(
          BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "sculk_pulser")))
  );
  public static final DuctBlock DUCT_BLOCK = new DuctBlock(
          BlockBehaviour.Properties.of().strength(1.0f).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "duct")))
  );
  public static final BuddingEchoBlock BUDDING_ECHO_BLOCK = new BuddingEchoBlock(
          BlockBehaviour.Properties.ofFullCopy(Blocks.BUDDING_AMETHYST).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "budding_echo")))
  );
  public static final EchoClusterBlock ECHO_CLUSTER_BLOCK = new EchoClusterBlock(
          7.0f,
          10.0f,
          BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "echo_cluster")))
  );
  public static final EchoClusterBlock LARGE_ECHO_BUD_BLOCK = new EchoClusterBlock(
          5.0f,
          10.0f,
          BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "large_echo_bud")))
  );
  public static final EchoClusterBlock MEDIUM_ECHO_BUD_BLOCK = new EchoClusterBlock(
          4.0f,
          10.0f,
          BlockBehaviour.Properties.ofFullCopy(Blocks.MEDIUM_AMETHYST_BUD).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "medium_echo_bud")))
  );
  public static final EchoClusterBlock SMALL_ECHO_BUD_BLOCK = new EchoClusterBlock(
          3.0f,
          8.0f,
          BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "small_echo_bud")))
  );

  public static BlockEntityType<LogicalComparatorBlockEntity> LOGICAL_COMPARATOR_BLOCK_ENTITY;
  public static BlockEntityType<SculkChamberBlockEntity> SCULK_CHAMBER_BLOCK_ENTITY;
  public static BlockEntityType<DuctBlockEntity> DUCT_BLOCK_ENTITY;

  public static final Identifier NOTE_BLOCK_SOUND_ID = Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID,"sculk_note_block_sound");
  public static SoundEvent NOTE_BLOCK_SOUND_EVENT = SoundEvent.createVariableRangeEvent(NOTE_BLOCK_SOUND_ID);

  public static final MenuType<DuctScreenHandler> DUCT_SCREEN_HANDLER = new MenuType<>(
    DuctScreenHandler::new,
    FeatureFlags.VANILLA_SET
  );

  private static void registerBlockItem(String path, Block block) {
    ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, path));
    ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, path));

    Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
  }

  private static <T extends BlockEntity> BlockEntityType<? extends T> registerBlockEntityItem(
    String path,
    Block block,
    BlockEntityType<? extends T> blockEntityType
  ) {
    registerBlockItem(path, block);

    return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, path), blockEntityType);
  }

  public static void initBlockItems() {
    // Blocks
    registerBlockItem("sculk_note_block", SCULK_NOTE_BLOCK);
    registerBlockItem("sculk_pulser", SCULK_PULSER_BLOCK);
    registerBlockItem("budding_echo", BUDDING_ECHO_BLOCK);
    registerBlockItem("echo_cluster", ECHO_CLUSTER_BLOCK);
    registerBlockItem("large_echo_bud", LARGE_ECHO_BUD_BLOCK);
    registerBlockItem("medium_echo_bud", MEDIUM_ECHO_BUD_BLOCK);
    registerBlockItem("small_echo_bud", SMALL_ECHO_BUD_BLOCK);

    // Block entities
    LOGICAL_COMPARATOR_BLOCK_ENTITY = (BlockEntityType<LogicalComparatorBlockEntity>) registerBlockEntityItem(
            "logical_comparator",
            LOGICAL_COMPARATOR_BLOCK,
            FabricBlockEntityTypeBuilder.create(LogicalComparatorBlockEntity::new, LOGICAL_COMPARATOR_BLOCK).build()
    );
    SCULK_CHAMBER_BLOCK_ENTITY = (BlockEntityType<SculkChamberBlockEntity>) registerBlockEntityItem(
            "sculk_chamber",
            SCULK_CHAMBER_BLOCK,
            FabricBlockEntityTypeBuilder.create(SculkChamberBlockEntity::new, SCULK_CHAMBER_BLOCK).build()
    );
    DUCT_BLOCK_ENTITY = (BlockEntityType<DuctBlockEntity>) registerBlockEntityItem(
            "duct",
            DUCT_BLOCK,
            FabricBlockEntityTypeBuilder.create(DuctBlockEntity::new, DUCT_BLOCK).build()
    );

    // Screen handlers
    Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "duct"), DUCT_SCREEN_HANDLER);

    // Sounds
    Registry.register(BuiltInRegistries.SOUND_EVENT, NOTE_BLOCK_SOUND_ID, NOTE_BLOCK_SOUND_EVENT);
  }

  public static void initCreativePlacement() {
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(content -> {
      content.insertAfter(Items.COMPARATOR, LOGICAL_COMPARATOR_BLOCK);
      content.insertAfter(LOGICAL_COMPARATOR_BLOCK, SCULK_CHAMBER_BLOCK);
      content.insertAfter(SCULK_CHAMBER_BLOCK, SCULK_NOTE_BLOCK);
      content.insertAfter(SCULK_NOTE_BLOCK, SCULK_PULSER_BLOCK);
    });
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(content -> {
      content.insertAfter(Items.SCULK_VEIN, BUDDING_ECHO_BLOCK);
      content.insertAfter(BUDDING_ECHO_BLOCK, ECHO_CLUSTER_BLOCK);
      content.insertAfter(ECHO_CLUSTER_BLOCK, LARGE_ECHO_BUD_BLOCK);
      content.insertAfter(LARGE_ECHO_BUD_BLOCK, MEDIUM_ECHO_BUD_BLOCK);
      content.insertAfter(MEDIUM_ECHO_BUD_BLOCK, SMALL_ECHO_BUD_BLOCK);
    });
  }
}
