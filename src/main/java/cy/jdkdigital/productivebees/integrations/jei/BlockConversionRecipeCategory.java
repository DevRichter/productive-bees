package cy.jdkdigital.productivebees.integrations.jei;

import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.common.recipe.BlockConversionRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;

public class BlockConversionRecipeCategory implements IRecipeCategory<BlockConversionRecipe>
{
    private final IDrawable background;
    private final IDrawable icon;

    public BlockConversionRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = new ResourceLocation(ProductiveBees.MODID, "textures/gui/jei/block_conversion.png");
        this.background = guiHelper.createDrawable(location, 0, 0, 90, 52);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(Items.COBBLESTONE));
    }

    @Nonnull
    @Override
    public ResourceLocation getUid() {
        return ProductiveBeesJeiPlugin.CATEGORY_BLOCK_CONVERSION_UID;
    }

    @Nonnull
    @Override
    public Class<? extends BlockConversionRecipe> getRecipeClass() {
        return BlockConversionRecipe.class;
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return new TranslatableComponent("jei.productivebees.block_conversion");
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Nonnull
    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BlockConversionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 38, 5)
                .addIngredient(ProductiveBeesJeiPlugin.BEE_INGREDIENT, recipe.bee.get())
                .setSlotName("source");

        if (!recipe.input.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 26)
                    .addItemStacks(Arrays.asList(recipe.input.getItems()))
                    .setSlotName("sourceBlocks");
        } else if (recipe.stateFrom.getFluidState().getType().equals(Fluids.EMPTY)) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 25)
                    .addItemStacks(Arrays.asList(recipe.fromDisplay.getItems()))
                    .setSlotName("sourceBlock");
        } else {
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 25)
                    .addIngredients(VanillaTypes.FLUID, Collections.singletonList(new FluidStack(recipe.stateFrom.getFluidState().getType(), 1000)))
                    .setSlotName("sourceFluid");
        }

        if (recipe.stateTo.getFluidState().getType().equals(Fluids.EMPTY)) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 65, 25)
                    .addItemStacks(Arrays.asList(recipe.toDisplay.getItems()))
                    .setSlotName("resultBlock");
        } else {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 65, 26)
                    .addIngredients(VanillaTypes.FLUID, Collections.singletonList(new FluidStack(recipe.stateTo.getFluidState().getType(), 1000)))
                    .setSlotName("resultFluid");
        }
    }
}
