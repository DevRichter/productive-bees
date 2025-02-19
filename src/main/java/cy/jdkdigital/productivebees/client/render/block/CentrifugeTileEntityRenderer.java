package cy.jdkdigital.productivebees.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import cy.jdkdigital.productivebees.ProductiveBeesConfig;
import cy.jdkdigital.productivebees.common.block.entity.CentrifugeBlockEntity;
import cy.jdkdigital.productivebees.common.block.entity.InventoryHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;

public class CentrifugeTileEntityRenderer implements BlockEntityRenderer<CentrifugeBlockEntity>
{
    public CentrifugeTileEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public void render(CentrifugeBlockEntity tileEntityIn, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if (ProductiveBeesConfig.CLIENT.renderCombsInCentrifuge.get()) {
            tileEntityIn.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(itemHandler -> {
                ItemStack stack = itemHandler.getStackInSlot(InventoryHandlerHelper.INPUT_SLOT);
                if (!stack.isEmpty()) {
                    ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
                    long time = System.currentTimeMillis();
                    double d = (time / 50) % 360;
                    int stackCount = stack.getCount();
                    double shownItemCount = stackCount < 20 ? stackCount : 20 + Math.ceil((stackCount - 20) / 4F);
                    for (int i = 0; i < shownItemCount; ++i) {
                        double angle = -d + 360D / shownItemCount * i;
                        double dX = Math.sin(Math.toRadians(angle)) * 0.25D;
                        double dZ = Math.cos(Math.toRadians(angle)) * 0.25D;

                        matrixStackIn.pushPose();
                        matrixStackIn.translate(0.5D + dX, 0.6375D, 0.5D + dZ);
                        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees((float) angle + 90F));
                        matrixStackIn.scale(0.35F, 0.35F, 0.35F);
                        ir.renderStatic(stack, ItemTransforms.TransformType.FIXED, combinedLightIn, combinedOverlayIn, matrixStackIn, bufferIn, 0);
                        matrixStackIn.popPose();
                    }
                }
            });
        }
    }
}