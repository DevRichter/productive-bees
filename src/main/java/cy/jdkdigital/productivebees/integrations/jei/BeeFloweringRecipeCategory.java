package cy.jdkdigital.productivebees.integrations.jei;

import com.google.common.collect.Streams;
import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.init.ModTags;
import cy.jdkdigital.productivebees.integrations.jei.ingredients.BeeIngredient;
import cy.jdkdigital.productivebees.setup.BeeReloadListener;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeeFloweringRecipeCategory implements IRecipeCategory<BeeFloweringRecipeCategory.Recipe>
{
    public static final ResourceLocation ID = new ResourceLocation(ProductiveBees.MODID, "bee_flowering");
    private final IDrawable icon;
    private final IDrawable background;

    public BeeFloweringRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = new ResourceLocation(ProductiveBees.MODID, "textures/gui/jei/bee_flowering_recipe.png");
        this.background = guiHelper.createDrawable(location, 0, 0, 70, 82);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(Items.POPPY));
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    @Override
    public Class<? extends Recipe> getRecipeClass() {
        return Recipe.class;
    }

    @Override
    public Component getTitle() {
        return new TranslatableComponent("jei.productivebees.bee_flowering");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 29, 12)
                .addIngredient(ProductiveBeesJeiPlugin.BEE_INGREDIENT, recipe.bee)
                .setSlotName("source");

        List<ItemStack> itemStacks = new ArrayList<>();
        List<FluidStack> fluidStacks = new ArrayList<>();
        try {
            List<Block> blockList = new ArrayList<>();
            if (recipe.blockTag != null) {
                blockList = Streams.stream(Registry.BLOCK.getTagOrEmpty(recipe.blockTag)).map(Holder::value).toList();
            } else if (recipe.fluidTag != null) {
                fluidStacks = Streams.stream(Registry.FLUID.getTagOrEmpty(recipe.fluidTag)).map(fluidHolder -> new FluidStack(fluidHolder.value(), 1000)).toList();
            } else if (recipe.block != null) {
                blockList.add(recipe.block);
            } else if (recipe.fluid != null) {
                fluidStacks.add(new FluidStack(recipe.fluid, 1000));
            }

            for (Block block : blockList) {
                ItemStack item = new ItemStack(block.asItem());
                if (!item.getItem().equals(Items.AIR)) {
                    itemStacks.add(item);
                } else {
                    if (block instanceof CocoaBlock) {
                        itemStacks.add(new ItemStack(Items.COCOA_BEANS));
                    }
                }
            }
        } catch (Exception e) {
            ProductiveBees.LOGGER.warn("Failed to find flowering requirements for " + recipe);
        }

        if (!fluidStacks.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 26, 51)
                    .addIngredients(VanillaTypes.FLUID, fluidStacks)
                    .setSlotName("inputFluid");
        }
        if (!itemStacks.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 26, 51)
                    .addItemStacks(itemStacks)
                    .setSlotName("inputItem");
        }
    }

    public static List<Recipe> getFlowersRecipes(Map<String, BeeIngredient> beeList) {
        List<Recipe> recipes = new ArrayList<>();

        // Hardcoded for now until bees are moved to config
        Map<String, TagKey<Block>> flowering = new HashMap<>();
        flowering.put("productivebees:blue_banded_bee", ModTags.RIVER_FLOWERS);
        flowering.put("productivebees:green_carpenter_bee", ModTags.FOREST_FLOWERS);
        flowering.put("productivebees:nomad_bee", ModTags.ARID_FLOWERS);
        flowering.put("productivebees:chocolate_mining_bee", ModTags.ARID_FLOWERS);
        flowering.put("productivebees:ashy_mining_bee", ModTags.ARID_FLOWERS);
        flowering.put("productivebees:reed_bee", ModTags.SWAMP_FLOWERS);
        flowering.put("productivebees:resin_bee", ModTags.FOREST_FLOWERS);
        flowering.put("productivebees:sweat_bee", ModTags.SNOW_FLOWERS);
        flowering.put("productivebees:yellow_black_carpenter_bee", ModTags.FOREST_FLOWERS);
        flowering.put("productivebees:lumber_bee", ModTags.LUMBER);
        flowering.put("productivebees:quarry_bee", ModTags.QUARRY);
        flowering.put("productivebees:creeper_bee", ModTags.POWDERY);

        TagKey<Block> defaultBlockTag = BlockTags.FLOWERS;

        for (Map.Entry<String, BeeIngredient> entry : beeList.entrySet()) {
            if (entry.getValue().isConfigurable()) {
                CompoundTag nbt = BeeReloadListener.INSTANCE.getData(entry.getValue().getBeeType().toString());
                if (nbt.contains("flowerTag")) {
                    TagKey<Block> flowerTag = ModTags.getBlockTag(new ResourceLocation(nbt.getString("flowerTag")));
                    recipes.add(Recipe.createBlock(flowerTag, entry.getValue()));
                } else if (nbt.contains("flowerBlock")) {
                    if (nbt.getString("flowerBlock").contains("#")) {
                        TagKey<Block> flowerTag = ModTags.getBlockTag(new ResourceLocation(nbt.getString("flowerBlock").replace("#", "")));
                        recipes.add(Recipe.createBlock(flowerTag, entry.getValue()));
                    } else {
                        Block flowerBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(nbt.getString("flowerBlock")));
                        recipes.add(Recipe.createBlock(flowerBlock, entry.getValue()));
                    }
                } else if (nbt.contains("flowerFluid")) {
                    if (nbt.getString("flowerFluid").contains("#")) {
                        TagKey<Fluid> flowerFluid = ModTags.getFluidTag(new ResourceLocation(nbt.getString("flowerFluid").replace("#", "")));
                        recipes.add(Recipe.createFluid(flowerFluid, entry.getValue()));
                    } else {
                        Fluid flowerFluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(nbt.getString("flowerFluid")));
                        recipes.add(Recipe.createFluid(flowerFluid, entry.getValue()));
                    }
                } else {
                    recipes.add(Recipe.createBlock(defaultBlockTag, entry.getValue()));
                }
            } else if (flowering.containsKey(entry.getValue().getBeeType().toString())) {
                TagKey<Block> blockTag = flowering.get(entry.getValue().getBeeType().toString());
                recipes.add(Recipe.createBlock(blockTag, entry.getValue()));
            } else {
                recipes.add(Recipe.createBlock(defaultBlockTag, entry.getValue()));
            }
        }
        return recipes;
    }

    public record Recipe(TagKey<Block> blockTag, Block block, TagKey<Fluid> fluidTag, Fluid fluid, BeeIngredient bee) {
        public static Recipe createBlock(TagKey<Block> blockTag, BeeIngredient bee) {
            return new Recipe(blockTag, null, null, null, bee);
        }

        public static Recipe createBlock(Block block, BeeIngredient bee) {
            return new Recipe(null, block, null, null, bee);
        }

        public static Recipe createFluid(TagKey<Fluid> fluidTag, BeeIngredient bee) {
            return new Recipe(null, null, fluidTag, null, bee);
        }

        public static Recipe createFluid(Fluid fluid, BeeIngredient bee) {
            return new Recipe(null, null, null, fluid, bee);
        }
    }
}
