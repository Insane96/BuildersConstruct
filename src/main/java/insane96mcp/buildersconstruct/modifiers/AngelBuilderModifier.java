package insane96mcp.buildersconstruct.modifiers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import insane96mcp.buildersconstruct.BCModifiers;
import insane96mcp.buildersconstruct.BuildersConstruct;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

@Mod.EventBusSubscriber(modid = BuildersConstruct.MOD_ID)
public class AngelBuilderModifier extends NoLevelsModifier implements GeneralInteractionModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT);
    }

    @Override
    public int getPriority() {
        return 101; //Run before construction. They shouldn't overlap, but just in case
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.isBroken()
                || player.isCrouching()
                || source != InteractionSource.RIGHT_CLICK
                //|| !tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)
                || !(player.getOffhandItem().getItem() instanceof BlockItem blockItemToPlace))
            return InteractionResult.PASS;

        if (!player.level().isClientSide) {
            Vec2 rotVector = player.getRotationVector();
            Vec3 eyePos = player.getEyePosition();
            double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
            Vec3 endRayCast = getEndRayCast(rotVector, eyePos, reach);
            HitResult hitResult = player.level().clip(new ClipContext(player.getEyePosition(), endRayCast, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, null));
            if (hitResult.getType() == HitResult.Type.BLOCK)
                return InteractionResult.PASS;
            BlockPos pos = BlockPos.containing(endRayCast);
            if (!player.level().getBlockState(pos).canBeReplaced())
                return InteractionResult.PASS;
            BlockPlaceContext blockPlaceContext = new BlockPlaceContext(player, InteractionHand.OFF_HAND, player.getItemInHand(InteractionHand.OFF_HAND), ConstructionModifier.createBlockHitResult(endRayCast, player.getDirection(), pos));
            InteractionResult interactionResult = blockItemToPlace.place(blockPlaceContext);
            if (interactionResult.consumesAction()) {
                BlockState placedState = player.level().getBlockState(blockPlaceContext.getClickedPos());
                SoundType soundtype = placedState.getSoundType(player.level(), blockPlaceContext.getClickedPos(), player);
                player.level().playSound(null, blockPlaceContext.getClickedPos(), placedState.getSoundType(player.level(), blockPlaceContext.getClickedPos(), player).getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

                if (ToolDamageUtil.directDamage(tool, 20, player, player.getItemInHand(hand))) {
                    player.broadcastBreakEvent(hand);
                }
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private static Vec3 getEndRayCast(Vec2 rotation, Vec3 startingPos, double reach) {
        float yCos = Mth.cos((rotation.y + 90.0F) * ((float) Math.PI / 180F));
        float ySin = Mth.sin((rotation.y + 90.0F) * ((float) Math.PI / 180F));
        float xCos = Mth.cos(-rotation.x * ((float) Math.PI / 180F));
        float xSin = Mth.sin(-rotation.x * ((float) Math.PI / 180F));
        Vec3 forwardVec = new Vec3(yCos * xCos, xSin, ySin * xCos);
        reach -= 1.5;
        double d0 = forwardVec.x * reach;
        double d1 = forwardVec.y * reach;
        double d2 = forwardVec.z * reach;
        return new Vec3(startingPos.x + d0, startingPos.y + d1, startingPos.z + d2);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !(event.getCamera().getEntity() instanceof LocalPlayer player))
            return;

        ToolStack stack;
        if (player.getMainHandItem().getItem() instanceof ModifiableItem)
            stack = ToolStack.from(player.getMainHandItem());
        else if (player.getOffhandItem().getItem() instanceof ModifiableItem)
            stack = ToolStack.from(player.getOffhandItem());
        else return;

        if (stack.isBroken())
            return;

        int angelBuilder = stack.getModifierLevel(BCModifiers.ANGEL_BUILDER.get());
        if (angelBuilder == 0)
            return;
        Vec2 rotVector = player.getRotationVector();
        Vec3 eyePos = player.getEyePosition();
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        Vec3 endRayCast = getEndRayCast(rotVector, eyePos, reach);
        HitResult hitResult = player.level().clip(new ClipContext(player.getEyePosition(), endRayCast, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, null));
        if (hitResult.getType() == HitResult.Type.BLOCK)
            return;
        BlockPos pos = BlockPos.containing(endRayCast);
        if (!player.level().getBlockState(pos).canBeReplaced())
            return;

        VertexConsumer vertexBuilder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.LINES);
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(-cam.x, -cam.y, -cam.z);

        LevelRenderer.renderShape(event.getPoseStack(), vertexBuilder, Shapes.block(), pos.getX(), pos.getY(), pos.getZ(), 0.67f, 0.89f, 0.91f, 0.667f);

        event.getPoseStack().popPose();
        RenderSystem.disableDepthTest();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }
}
