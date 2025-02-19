package cy.jdkdigital.productivebees.common.item;

import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.init.ModItems;
import cy.jdkdigital.productivebees.util.BeeAttribute;
import cy.jdkdigital.productivebees.util.BeeAttributes;
import cy.jdkdigital.productivebees.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class Gene extends Item
{
    public static final String ATTRIBUTE_KEY = "productivebees_gene_attribute";
    public static final String VALUE_KEY = "productivebees_gene_value";
    public static final String PURITY_KEY = "productivebees_gene_purity";

    public static float color(ItemStack itemStack) {
        return switch (getAttributeName(itemStack)) {
            case "productivity" -> 0.1F;
            case "endurance" -> 0.2F;
            case "temper" -> 0.3F;
            case "behavior" -> 0.4F;
            case "weather_tolerance" -> 0.5F;
            default -> 0.0F;
        };
    }

    public Gene(Properties properties) {
        super(properties);
    }

    public static ItemStack getStack(BeeAttribute<?> attribute, int value) {
        return getStack(attribute, value, 1);
    }

    public static ItemStack getStack(BeeAttribute<?> attribute, int value, int count) {
        return getStack(attribute, value, count, ProductiveBees.rand.nextInt(40) + 15);
    }

    public static ItemStack getStack(BeeAttribute<?> attribute, int value, int count, int purity) {
        return getStack(attribute.toString(), value, count, purity);
    }

    public static ItemStack getStack(String type, int value) {
        return getStack(type, 0, 1, value);
    }

    public static ItemStack getStack(String attribute, int value, int count, int purity) {
        ItemStack result = new ItemStack(ModItems.GENE.get(), count);
        setAttribute(result, attribute, value, purity);
        return result;
    }

    public static void setAttribute(ItemStack stack, String attributeId, int value, int purity) {
        stack.getOrCreateTag().putString(ATTRIBUTE_KEY, attributeId);
        stack.getOrCreateTag().putInt(VALUE_KEY, value);
        stack.getOrCreateTag().putInt(PURITY_KEY, purity);
    }

    @Nullable
    public static BeeAttribute<?> getAttribute(ItemStack stack) {
        String name = getAttributeName(stack);
        return BeeAttributes.getAttributeByName(name);
    }

    public static String getAttributeName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(ATTRIBUTE_KEY) : "";
    }

    public static Integer getValue(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(VALUE_KEY) : 0;
    }

    public static Integer getPurity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(PURITY_KEY) : 0;
    }

    public static void setPurity(ItemStack stack, int purity) {
        stack.getOrCreateTag().putInt(PURITY_KEY, purity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, world, list, flag);

        Integer value = getValue(stack);

        BeeAttribute<?> attribute = getAttribute(stack);

        if (attribute != null && BeeAttributes.keyMap.containsKey(attribute) && BeeAttributes.keyMap.get(attribute).containsKey(value)) {
            Component translatedValue = new TranslatableComponent(BeeAttributes.keyMap.get(attribute).get(value)).withStyle(ColorUtil.getColor(value));
            list.add((new TranslatableComponent("productivebees.information.attribute." + getAttributeName(stack), translatedValue)).withStyle(ChatFormatting.DARK_GRAY).append(new TextComponent(" (" + getPurity(stack) + "%)")));
        } else {
            String type = getAttributeName(stack);
            list.add(new TranslatableComponent("productivebees.information.attribute.type", type).withStyle(ChatFormatting.GOLD).append(new TextComponent(" (" + getPurity(stack) + "%)")));
        }
    }

    @Override
    public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> items) {
        if (this.allowdedIn(group)) {
            items.add(getStack(BeeAttributes.PRODUCTIVITY, 0, 1, 100));
            items.add(getStack(BeeAttributes.PRODUCTIVITY, 1, 1, 100));
            items.add(getStack(BeeAttributes.PRODUCTIVITY, 2, 1, 100));
            items.add(getStack(BeeAttributes.PRODUCTIVITY, 3, 1, 100));
            items.add(getStack(BeeAttributes.WEATHER_TOLERANCE, 0, 1, 100));
            items.add(getStack(BeeAttributes.WEATHER_TOLERANCE, 1, 1, 100));
            items.add(getStack(BeeAttributes.WEATHER_TOLERANCE, 2, 1, 100));
            items.add(getStack(BeeAttributes.BEHAVIOR, 0, 1, 100));
            items.add(getStack(BeeAttributes.BEHAVIOR, 1, 1, 100));
            items.add(getStack(BeeAttributes.BEHAVIOR, 2, 1, 100));
            items.add(getStack(BeeAttributes.TEMPER, 0, 1, 100));
            items.add(getStack(BeeAttributes.TEMPER, 1, 1, 100));
            items.add(getStack(BeeAttributes.TEMPER, 2, 1, 100));
            items.add(getStack(BeeAttributes.TEMPER, 3, 1, 100));
            items.add(getStack(BeeAttributes.ENDURANCE, 0, 1, 100));
            items.add(getStack(BeeAttributes.ENDURANCE, 1, 1, 100));
            items.add(getStack(BeeAttributes.ENDURANCE, 2, 1, 100));
            items.add(getStack(BeeAttributes.ENDURANCE, 3, 1, 100));
        }
    }
}
