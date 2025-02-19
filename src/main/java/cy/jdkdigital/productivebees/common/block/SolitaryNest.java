package cy.jdkdigital.productivebees.common.block;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.ProductiveBeesConfig;
import cy.jdkdigital.productivebees.common.block.entity.SolitaryNestBlockEntity;
import cy.jdkdigital.productivebees.common.entity.bee.ConfigurableBee;
import cy.jdkdigital.productivebees.common.item.HoneyTreat;
import cy.jdkdigital.productivebees.common.recipe.BeeSpawningRecipe;
import cy.jdkdigital.productivebees.init.ModBlocks;
import cy.jdkdigital.productivebees.init.ModRecipeTypes;
import cy.jdkdigital.productivebees.init.ModTileEntityTypes;
import cy.jdkdigital.productivebees.integrations.jei.ingredients.BeeIngredient;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SolitaryNest extends AdvancedBeehiveAbstract
{
    public static Supplier<BiMap<Block, Block>> BLOCK_TO_NEST = Suppliers.memoize(() -> ImmutableBiMap.<Block, Block>builder()
            .put(Blocks.OAK_LOG, ModBlocks.OAK_WOOD_NEST.get())
            .put(Blocks.BIRCH_LOG, ModBlocks.BIRCH_WOOD_NEST.get())
            .put(Blocks.SPRUCE_LOG, ModBlocks.SPRUCE_WOOD_NEST.get())
            .put(Blocks.ACACIA_LOG, ModBlocks.ACACIA_WOOD_NEST.get())
            .put(Blocks.DARK_OAK_LOG, ModBlocks.DARK_OAK_WOOD_NEST.get())
            .put(Blocks.JUNGLE_LOG, ModBlocks.JUNGLE_WOOD_NEST.get())
            .put(Blocks.GLOWSTONE, ModBlocks.GLOWSTONE_NEST.get())
            .put(Blocks.NETHER_QUARTZ_ORE, ModBlocks.NETHER_QUARTZ_NEST.get())
            .put(Blocks.NETHER_BRICKS, ModBlocks.NETHER_BRICK_NEST.get())
            .put(Blocks.GOLD_ORE, ModBlocks.NETHER_GOLD_NEST.get())
            .put(Blocks.END_STONE, ModBlocks.END_NEST.get())
            .put(Blocks.OBSIDIAN, ModBlocks.OBSIDIAN_PILLAR_NEST.get())
            .put(Blocks.SLIME_BLOCK, ModBlocks.SLIMY_NEST.get())
            .put(Blocks.SUGAR_CANE, ModBlocks.SUGAR_CANE_NEST.get())
            .put(Blocks.DIRT, ModBlocks.COARSE_DIRT_NEST.get())
            .put(Blocks.STONE, ModBlocks.STONE_NEST.get())
            .put(Blocks.SAND, ModBlocks.SAND_NEST.get())
            .put(Blocks.SNOW_BLOCK, ModBlocks.SNOW_NEST.get())
            .put(Blocks.GRAVEL, ModBlocks.GRAVEL_NEST.get())
        .build());
    List<BeeSpawningRecipe> recipes = new ArrayList<>();

    public SolitaryNest(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ModTileEntityTypes.SOLITARY_NEST.get(), SolitaryNestBlockEntity::tick);
    }

    public int getMaxHoneyLevel() {
        return 0;
    }

    public Entity getNestingBeeType(Level level, Biome biome) {
        List<BeeSpawningRecipe> spawningRecipes = getSpawningRecipes(level, biome);
        if (!spawningRecipes.isEmpty()) {
            BeeSpawningRecipe spawningRecipe = spawningRecipes.get(level.random.nextInt(spawningRecipes.size()));
            BeeIngredient beeIngredient = spawningRecipe.output.get(level.random.nextInt(spawningRecipe.output.size())).get();
            if (beeIngredient != null) {
                Entity bee = beeIngredient.getBeeEntity().create(level);
                if (bee instanceof ConfigurableBee) {
                    ((ConfigurableBee) bee).setBeeType(beeIngredient.getBeeType().toString());
                    ((ConfigurableBee) bee).setAttributes();
                }
                return bee;
            } else {
                ProductiveBees.LOGGER.debug("No bee ingredient found in " + spawningRecipe);
            }
        }
        return null;
    }

    public List<BeeSpawningRecipe> getSpawningRecipes(Level world, Biome biome) {
        // Get and cache recipes for nest type
        if (recipes.isEmpty()) {
            Map<ResourceLocation, Recipe<Container>> allRecipes = new HashMap<>();
            allRecipes.putAll(world.getRecipeManager().byType(ModRecipeTypes.BEE_SPAWNING_BIG_TYPE));
            allRecipes.putAll(world.getRecipeManager().byType(ModRecipeTypes.BEE_SPAWNING_TYPE));
            ItemStack nestItem = new ItemStack(ForgeRegistries.ITEMS.getValue(this.getRegistryName()));
            for (Map.Entry<ResourceLocation, Recipe<Container>> entry : allRecipes.entrySet()) {
                BeeSpawningRecipe recipe = (BeeSpawningRecipe) entry.getValue();
                if (recipe.matches(nestItem)) {
                    recipes.add(recipe);
                }
            }
        }

        List<BeeSpawningRecipe> spawningRecipes = new ArrayList<>();
        if (!recipes.isEmpty()) {
            for (BeeSpawningRecipe recipe : recipes) {
                if (
                        (recipe.biomes.isEmpty() && world.dimension() == Level.OVERWORLD) || recipe.biomes.contains(biome.getBiomeCategory().getName())
                ) {
                    spawningRecipes.add(recipe);
                }
            }
        }
        return spawningRecipes;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolitaryNestBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext itemUseContext) {
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, itemUseContext.getNearestLookingDirection().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
    }

    public boolean canRepopulateIn(Level world, Biome biome) {
        return !getSpawningRecipes(world, biome).isEmpty();
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide()) {
            SolitaryNestBlockEntity tileEntity = (SolitaryNestBlockEntity) world.getBlockEntity(pos);

            ItemStack heldItem = player.getItemInHand(hand);
            if (tileEntity != null && heldItem.getItem() instanceof HoneyTreat && !HoneyTreat.hasGene(heldItem)) {
                boolean itemUse = false;
                int currentCooldown = tileEntity.getNestTickCooldown();
                if (tileEntity.canRepopulate()) {
                    itemUse = true;
                    if (currentCooldown <= 0) {
                        tileEntity.setNestCooldown(ProductiveBeesConfig.GENERAL.nestSpawnCooldown.get());
                    } else {
                        tileEntity.setNestCooldown((int) (currentCooldown * 0.9));
                    }
                }

                if (itemUse) {
                    world.levelEvent(2005, pos, 0);
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer) player, pos, heldItem);

                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }
            }
        }

        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

        if (stack.hasTag() && stack.getTag().getInt("spawnCount") >= ProductiveBeesConfig.BEES.cuckooSpawnCount.get()) {
            tooltip.add(new TranslatableComponent("productivebees.hive.tooltip.nest_inactive").withStyle(ChatFormatting.BOLD));
        }
    }
}
