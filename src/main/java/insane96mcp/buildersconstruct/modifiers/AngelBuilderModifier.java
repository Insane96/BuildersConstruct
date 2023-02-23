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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap;
import slimeknights.tconstruct.library.tools.definition.module.ToolModuleHooks;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

@Mod.EventBusSubscriber(modid = BuildersConstruct.MOD_ID)
public class AngelBuilderModifier extends NoLevelsModifier implements GeneralInteractionModifierHook {

    @Override
    protected void registerHooks(ModifierHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, TinkerHooks.CHARGEABLE_INTERACT);
    }

    @Override
    public int getPriority() {
        return 101; //Run before construction. They shouldn't overlap, but just in case
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.getCurrentDurability() < 1
                || player.isCrouching()
                || !tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)
                || !(player.getOffhandItem().getItem() instanceof BlockItem blockItemToPlace))
            return InteractionResult.PASS;

        if (!player.getLevel().isClientSide) {
            Vec2 vec2 = player.getRotationVector();
            Vec3 vec3 = player.getEyePosition();
            float yCos = Mth.cos((vec2.y + 90.0F) * ((float)Math.PI / 180F));
            float ySin = Mth.sin((vec2.y + 90.0F) * ((float)Math.PI / 180F));
            float xCos = Mth.cos(-vec2.x * ((float)Math.PI / 180F));
            float xSin = Mth.sin(-vec2.x * ((float)Math.PI / 180F));
            Vec3 forwardVec = new Vec3(yCos * xCos, xSin, ySin * xCos);
            double d0 = forwardVec.x * 3;
            double d1 = forwardVec.y * 3;
            double d2 = forwardVec.z * 3;
            Vec3 endRayCast = new Vec3(vec3.x + d0, vec3.y + d1, vec3.z + d2);
            HitResult hitResult = player.level.clip(new ClipContext(player.getEyePosition(), endRayCast, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, null));
            if (hitResult.getType() == HitResult.Type.BLOCK)
                return InteractionResult.PASS;
            BlockPos pos = new BlockPos(endRayCast);
            if (player.level.getBlockState(pos).canOcclude())
                return InteractionResult.PASS;
            player.level.setBlock(pos, blockItemToPlace.getBlock().defaultBlockState(), 3);
            if (ToolDamageUtil.directDamage(tool, 20, player, player.getItemInHand(hand))) {
                player.broadcastBreakEvent(hand);
            }
        }
        return InteractionResult.sidedSuccess(player.level.isClientSide);
    }

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

        int angelBuilder = stack.getModifierLevel(BCModifiers.ANGEL_BUILDER.get());
        if (angelBuilder == 0)
            return;
        Vec2 vec2 = player.getRotationVector();
        Vec3 vec3 = player.getEyePosition();
        float yCos = Mth.cos((vec2.y + 90.0F) * ((float)Math.PI / 180F));
        float ySin = Mth.sin((vec2.y + 90.0F) * ((float)Math.PI / 180F));
        float xCos = Mth.cos(-vec2.x * ((float)Math.PI / 180F));
        float xSin = Mth.sin(-vec2.x * ((float)Math.PI / 180F));
        Vec3 forwardVec = new Vec3(yCos * xCos, xSin, ySin * xCos);
        double d0 = forwardVec.x * 3;
        double d1 = forwardVec.y * 3;
        double d2 = forwardVec.z * 3;
        Vec3 endRayCast = new Vec3(vec3.x + d0, vec3.y + d1, vec3.z + d2);
        HitResult hitResult = player.level.clip(new ClipContext(player.getEyePosition(), endRayCast, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, null));
        if (hitResult.getType() == HitResult.Type.BLOCK)
            return;
        BlockPos pos = new BlockPos(endRayCast);
        if (player.level.getBlockState(pos).canOcclude())
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
