package cy.jdkdigital.productivebees.integrations.jei;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.init.ModBlocks;
import cy.jdkdigital.productivebees.recipe.CentrifugeRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class CentrifugeRecipeCategory implements IRecipeCategory<CentrifugeRecipe>
{
    private final IDrawable background;
    private final IDrawable icon;

    public CentrifugeRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = new ResourceLocation(ProductiveBees.MODID, "textures/gui/jei/centrifuge_recipe.png");
        this.background = guiHelper.createDrawable(location, 0, 0, 126, 70);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModBlocks.CENTRIFUGE.get()));
    }

    @Nonnull
    @Override
    public ResourceLocation getUid() {
        return ProductiveBeesJeiPlugin.CATEGORY_CENTRIFUGE_UID;
    }

    @Nonnull
    @Override
    public Class<? extends CentrifugeRecipe> getRecipeClass() {
        return CentrifugeRecipe.class;
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return new TranslatableComponent("jei.productivebees.centrifuge");
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Nonnull
    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setIngredients(@Nonnull CentrifugeRecipe recipe, @Nonnull IIngredients ingredients) {
        ingredients.setInputIngredients(Lists.newArrayList(recipe.ingredient));

        List<List<ItemStack>> outputList = new ArrayList<>();

//        recipe.itemOutput.forEach((ingredient, intNBTS) -> {
//            List<ItemStack> stacks = Arrays.asList(ingredient.getItems());
//            for (ItemStack stack: stacks) {
//
//            }
//        });

        recipe.getRecipeOutputs().forEach((stack, value) -> {
            List<ItemStack> innerList = new ArrayList<>();
            IntStream.range(value.get(0).getAsInt(), value.get(1).getAsInt() + 1).forEach((i) -> {
                ItemStack newStack = stack.copy();
                newStack.setCount(i);
                innerList.add(newStack);
            });
            outputList.add(innerList);
        });

        ingredients.setOutputLists(VanillaTypes.ITEM, outputList);

        Pair<Fluid, Integer> fluid = recipe.getFluidOutputs();
        if (fluid != null && fluid.getSecond() > 0) {
            ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(fluid.getFirst(), fluid.getSecond()));
        }
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayout recipeLayout, @Nonnull CentrifugeRecipe recipe, @Nonnull IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        IGuiFluidStackGroup fluidStacks = recipeLayout.getFluidStacks();

        // Input item
        itemStacks.init(0, true, 4, 26);

        // Output items
        int startX = 68;
        int startY = 26;
        int offset = ingredients.getInputs(VanillaTypes.ITEM).size();
        List<List<ItemStack>> itemOutputs = ingredients.getOutputs(VanillaTypes.ITEM);
        if (itemOutputs.size() > 0) {
            IntStream.range(0, itemOutputs.size()).forEach((i) -> {
                itemStacks.init(i + offset, false, startX + (i * 18), startY + ((int) Math.floor(((float) i) / 3.0F) * 18));
            });
        }
        itemStacks.set(ingredients);

        // Output fluids
        List<List<FluidStack>> fluidOutputs = ingredients.getOutputs(VanillaTypes.FLUID);
        if (fluidOutputs.size() > 0) {
            IntStream.range(itemOutputs.size(), itemOutputs.size() + fluidOutputs.size()).forEach((i) -> {
                fluidStacks.init(i + offset, false, startX + (i * 18) + 1, startY + ((int) Math.floor(((float) i) / 3.0F) * 18) + 1);
            });
        }
        fluidStacks.set(ingredients);

        List<Component> chances = new ArrayList<>();
        List<Component> amounts = new ArrayList<>();
        recipe.getRecipeOutputs().forEach((stack, value) -> {
            int chance = value.get(2).getAsInt();
            if (chance < 100) {
                chances.add(new TranslatableComponent("productivebees.centrifuge.tooltip.chance", chance < 1 ? "<1%" : chance + "%"));
            } else {
                chances.add(new TextComponent(""));
            }
            if (value.get(0) != value.get(1)) {
                amounts.add(new TranslatableComponent("productivebees.centrifuge.tooltip.amount", value.get(0).getAsInt() + " - " + value.get(1).getAsInt()));
            } else {
                amounts.add(new TextComponent(""));
            }
        });
        Pair<Fluid, Integer> fluid = recipe.getFluidOutputs();
        if (fluid != null) {
            chances.add(new TextComponent(""));
            amounts.add(new TranslatableComponent("productivebees.centrifuge.tooltip.amount", fluid.getSecond() + "mb"));
        }

        itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (!input) {
                if (!chances.isEmpty() && chances.size() >= slotIndex && !chances.get(slotIndex - 1).getString().isEmpty()) {
                    tooltip.add(chances.get(slotIndex - 1));
                }
                if (!amounts.isEmpty() && amounts.size() >= slotIndex && !amounts.get(slotIndex - 1).getString().isEmpty()) {
                    tooltip.add(amounts.get(slotIndex - 1));
                }
            }
        });
        fluidStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (!chances.toString().isEmpty() && chances.size() >= slotIndex && !chances.get(slotIndex - 1).getString().isEmpty()) {
                tooltip.add(chances.get(slotIndex - 1));
            }
            if (!amounts.toString().isEmpty() && amounts.size() >= slotIndex && !amounts.get(slotIndex - 1).getString().isEmpty()) {
                tooltip.add(amounts.get(slotIndex - 1));
            }
        });
    }
}
