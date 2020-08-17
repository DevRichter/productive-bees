package cy.jdkdigital.productivebees.setup;

import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.block.CombBlock;
import cy.jdkdigital.productivebees.client.render.block.BottlerTileEntityRenderer;
import cy.jdkdigital.productivebees.client.render.block.CentrifugeTileEntityRenderer;
import cy.jdkdigital.productivebees.container.gui.AdvancedBeehiveScreen;
import cy.jdkdigital.productivebees.container.gui.BottlerScreen;
import cy.jdkdigital.productivebees.container.gui.CentrifugeScreen;
import cy.jdkdigital.productivebees.init.*;
import cy.jdkdigital.productivebees.item.HoneyComb;
import cy.jdkdigital.productivebees.item.SpawnEgg;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.world.biome.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ProductiveBees.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup
{
    public static void init(final FMLClientSetupEvent event) {
        ScreenManager.registerFactory(ModContainerTypes.ADVANCED_BEEHIVE.get(), AdvancedBeehiveScreen::new);
        ScreenManager.registerFactory(ModContainerTypes.CENTRIFUGE.get(), CentrifugeScreen::new);
        ScreenManager.registerFactory(ModContainerTypes.POWERED_CENTRIFUGE.get(), CentrifugeScreen::new);
        ScreenManager.registerFactory(ModContainerTypes.BOTTLER.get(), BottlerScreen::new);

        ClientRegistry.bindTileEntityRenderer(ModTileEntityTypes.CENTRIFUGE.get(), CentrifugeTileEntityRenderer::new);
        ClientRegistry.bindTileEntityRenderer(ModTileEntityTypes.POWERED_CENTRIFUGE.get(), CentrifugeTileEntityRenderer::new);
        ClientRegistry.bindTileEntityRenderer(ModTileEntityTypes.BOTTLER.get(), BottlerTileEntityRenderer::new);

        ModEntities.registerRendering();
        ModBlocks.registerRendering();
    }

    public static void registerItemColors(final ColorHandlerEvent.Item event) {
        ItemColors colors = event.getItemColors();
        for (RegistryObject<Item> eggItem : ModItems.SPAWN_EGGS) {
            if (ObfuscationReflectionHelper.getPrivateValue(RegistryObject.class, eggItem, "value") != null) {
                Item item = eggItem.get();
                if (item instanceof SpawnEgg) {
                    colors.register((itemColor, tintIndex) -> ((SpawnEgg) item).getColor(tintIndex), item);
                }
            }
        }

        // Honeycomb colors
        for (RegistryObject<Item> registryItem : ModItems.ITEMS.getEntries()) {
            Item item = registryItem.get();
            if (item instanceof HoneyComb) {
                ProductiveBees.LOGGER.info("Setting item color for " + item + " to " + ((HoneyComb) item).getColor());
                colors.register((itemColor, tintIndex) -> ((HoneyComb) item).getColor(), item);
            }
        }
    }

    public static void registerBlockColors(final ColorHandlerEvent.Block event) {
        BlockColors colors = event.getBlockColors();
        colors.register((blockState, lightReader, pos, tintIndex) -> {
            return lightReader != null && pos != null ? BiomeColors.getGrassColor(lightReader, pos) : -1;
        }, ModBlocks.SUGAR_CANE_NEST.get());

        for (RegistryObject<Block> registryBlock : ModBlocks.BLOCKS.getEntries()) {
            Block block = registryBlock.get();
            if (block instanceof CombBlock) {
                ProductiveBees.LOGGER.info("Setting block color for " + block + " to " + ((CombBlock) block).getColor());
                colors.register((blockState, lightReader, pos, tintIndex) -> ((CombBlock) block).getColor(), block);
            }
        }
    }
}
