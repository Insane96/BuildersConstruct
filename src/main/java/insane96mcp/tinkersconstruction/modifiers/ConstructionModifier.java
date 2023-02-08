package insane96mcp.tinkersconstruction.modifiers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import insane96mcp.tinkersconstruction.TinkersConstruction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap;
import slimeknights.tconstruct.library.tools.definition.module.ToolModuleHooks;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.*;

@Mod.EventBusSubscriber(modid = TinkersConstruction.MOD_ID)
public class ConstructionModifier extends Modifier implements BlockInteractionModifierHook {

    private static final int[] EXPANDED_LEVEL_RANGE = {5, 9, 16, 32, 64, 128, 256, 512, 1024, 2048};

    private static Map<Direction, List<Direction>> DIRECTION_CLOCKWISE;

    @Override
    protected void registerHooks(ModifierHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, TinkerHooks.BLOCK_INTERACT);

        DIRECTION_CLOCKWISE = new EnumMap<>(Direction.class);
        DIRECTION_CLOCKWISE.put(Direction.UP, Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.DOWN, Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.NORTH, Arrays.asList(Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.SOUTH, Arrays.asList(Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.EAST, Arrays.asList(Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH));
        DIRECTION_CLOCKWISE.put(Direction.WEST, Arrays.asList(Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH));
    }

    @Override
    public Component getDisplayName(IToolStackView tool, int level) {
        return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, level));
    }

    @Override
    public InteractionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        if (tool.getCurrentDurability() > 1 && tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            Player player = context.getPlayer();
            if (!context.getLevel().isClientSide) {
                Level level = context.getLevel();
                Direction face = context.getClickedFace();
                BlockState stateClicked = level.getBlockState(context.getClickedPos());
                BlockPos mainPos = context.getClickedPos();
                if (!level.getBlockState(mainPos.relative(face)).isAir())
                    return InteractionResult.PASS;

                int expandedLevel = tool.getModifierLevel(TinkerModifiers.expanded.get());
                if (expandedLevel > EXPANDED_LEVEL_RANGE.length)
                    expandedLevel = EXPANDED_LEVEL_RANGE.length - 1;
                int placeableAmount = EXPANDED_LEVEL_RANGE[expandedLevel];
                List<BlockPos> toCheck = new ArrayList<>();
                toCheck.add(mainPos);
                List<BlockPos> checked = new ArrayList<>();
                int placed = 0;
                while (!toCheck.isEmpty() && placed < placeableAmount) {
                    List<BlockPos> newList = new ArrayList<>();
                    for (BlockPos pos : toCheck) {
                        if (level.setBlock(pos.relative(face), stateClicked, 3)) {
                            placed++;
                            for (Direction dir : DIRECTION_CLOCKWISE.get(face)){
                                BlockPos newPos = pos.relative(dir);
                                if (checked.contains(newPos) || !level.getBlockState(newPos).equals(stateClicked))
                                    continue;
                                newList.add(newPos);
                            }
                            if (placed >= placeableAmount) {
                                break;
                            }
                        }
                    }
                    checked.addAll(toCheck);
                    toCheck = new ArrayList<>(newList);
                }

                /*Direction clockWiseSide = face.getClockWise();
                Direction counterClockWiseSide = face.getCounterClockWise();
                int i = 0;
                do {
                    BlockPos pos;
                    if (i % 2 == 0) {
                        pos = context.getClickedPos().relative(clockWiseSide, (i + 1) / 2);
                        if (level.getBlockState(pos).equals(stateClicked)) {
                            if (level.setBlock(pos.relative(face), stateClicked, 3)) {
                                placed++;
                            }
                        }
                    }
                    if (i % 2 == 1) {
                        pos = context.getClickedPos().relative(counterClockWiseSide, (i + 1) / 2);
                        if (level.getBlockState(pos).equals(stateClicked)) {
                            if (level.setBlock(pos.relative(face), stateClicked, 3)) {
                                placed++;
                            }
                        }
                    }
                    i++;
                } while(placed < placeableAmount && i < 2048);
*/
                if (ToolDamageUtil.damage(tool, placed, player, context.getItemInHand()) && player != null) {
                    player.broadcastBreakEvent(source.getSlot(context.getHand()));
                }
                level.playSound(null, mainPos, stateClicked.getSoundType(level, mainPos, player).getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !(event.getCamera().getEntity() instanceof LocalPlayer player)
                || Minecraft.getInstance().hitResult == null
                || Minecraft.getInstance().hitResult.getType() != HitResult.Type.BLOCK)
            return;

        if (player.getMainHandItem().getItem() instanceof ModifiableItem) {
            Level level = player.getLevel();
            BlockHitResult blockhitresult = (BlockHitResult)Minecraft.getInstance().hitResult;
            Direction face = blockhitresult.getDirection();
            BlockPos pos = blockhitresult.getBlockPos().relative(face);
            BlockPos playerPos = player.blockPosition();
            int dx = playerPos.getX() - pos.getX();
            int dz = playerPos.getZ() - pos.getZ();
            if ((dx * dx + dz * dz) < 512) {
                VertexConsumer vertexBuilder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.LINES);
                Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                event.getPoseStack().translate(-cam.x, -cam.y, -cam.z);
                event.getPoseStack().pushPose();
                LevelRenderer.renderShape(event.getPoseStack(), vertexBuilder, Shapes.block(), pos.getX(), pos.getY(), pos.getZ(), 1f, 0f, 0f, 1.0f);
                event.getPoseStack().popPose();
                RenderSystem.disableDepthTest();
                Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            }
        }
    }
}
