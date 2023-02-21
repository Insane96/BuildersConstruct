package insane96mcp.buildersconstruct.modifiers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import insane96mcp.buildersconstruct.BCModifiers;
import insane96mcp.buildersconstruct.BuildersConstruct;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap;
import slimeknights.tconstruct.library.tools.definition.module.ToolModuleHooks;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.TooltipKey;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.*;

@Mod.EventBusSubscriber(modid = BuildersConstruct.MOD_ID)
public class ConstructionModifier extends NoLevelsModifier implements BlockInteractionModifierHook, GeneralInteractionModifierHook {

    private static final UUID UUID = java.util.UUID.fromString("6f4a1c1e-423f-4059-a258-4e8e747f847e");

    private static final int[] EXPANDED_LEVEL_RANGE = {5, 9, 16, 32, 64, 128, 256, 512, 1024, 2048};

    private static final ResourceLocation MODE = new ResourceLocation(BuildersConstruct.MOD_ID, "construction_mode");

    private static Map<Direction, List<Direction>> DIRECTION_CLOCKWISE;

    @Override
    protected void registerHooks(ModifierHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, TinkerHooks.BLOCK_INTERACT, TinkerHooks.CHARGEABLE_INTERACT);

        DIRECTION_CLOCKWISE = new EnumMap<>(Direction.class);
        DIRECTION_CLOCKWISE.put(Direction.UP, Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.DOWN, Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.NORTH, Arrays.asList(Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.SOUTH, Arrays.asList(Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.EAST, Arrays.asList(Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH));
        DIRECTION_CLOCKWISE.put(Direction.WEST, Arrays.asList(Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH));
    }

    public void nextMode(IToolStackView tool) {
        int index = getMode(tool).ordinal();
        index++;
        if (index >= Mode.values.length)
            index = 0;
        setMode(tool, Mode.values[index]);
    }

    public Mode getMode(IToolStackView tool) {
        return Mode.values[tool.getPersistentData().getInt(MODE)];
    }

    public void setMode(IToolStackView tool, Mode mode) {
        tool.getPersistentData().putInt(MODE, mode.ordinal());
    }

    @Override
    public Component getDisplayName(IToolStackView tool, int level) {
        return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, level));
    }

    @Override
    public InteractionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        if (tool.getCurrentDurability() <= 1
                || !tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)
                || context.getPlayer() == null)
            return InteractionResult.PASS;

        Player player = context.getPlayer();
        //Only run the block placing server side
        if (!context.getLevel().isClientSide) {
            Level level = context.getLevel();
            Direction face = context.getClickedFace();
            BlockState stateToPlace = level.getBlockState(context.getClickedPos());
            Item blockItemToPlace;
            boolean offHandBlockPlacing = false;
            if (player.getOffhandItem().getItem() instanceof BlockItem) {
                blockItemToPlace = player.getOffhandItem().getItem();
                BlockPlaceContext blockPlaceContext = new BlockPlaceContext(player, context.getHand(), player.getItemInHand(context.getHand()), createBlockHitResult(context.getClickLocation(), face, context.getClickedPos()));
                stateToPlace = ((BlockItem) blockItemToPlace).getBlock().getStateForPlacement(blockPlaceContext);
                offHandBlockPlacing = true;
            }
            else {
                blockItemToPlace = stateToPlace.getBlock().asItem();
            }
            BlockPos mainPos = context.getClickedPos();
            //Place blocks only if the block on the face is air
            if (!level.getBlockState(mainPos.relative(face)).getMaterial().isReplaceable())
                return InteractionResult.PASS;

            //Calculate how many blocks the player has
            int blockCount;
            //If in creative set -1 (infinite blocks)
            if (player.isCreative())
                blockCount = -1;
            else {
                //Otherwise use the /clear command count feature
                blockCount = ContainerHelper.clearOrCountMatchingItems(player.getInventory(), itemStack -> itemStack.getItem().equals(blockItemToPlace), 0, true);
            }
            if (blockCount == 0)
                return InteractionResult.PASS;

            int expandedLevel = tool.getModifierLevel(TinkerModifiers.expanded.get());
            List<BlockPos> blocksToPlace = getBlocksToLay(level, mainPos, stateToPlace, !offHandBlockPlacing, face, expandedLevel, this.getMode(tool));
            int placed = 0;
            for (BlockPos pos : blocksToPlace) {
                pos = pos.relative(face);
                if (offHandBlockPlacing) {
                    BlockPlaceContext blockPlaceContext = new BlockPlaceContext(player, context.getHand(), player.getItemInHand(context.getHand()), createBlockHitResult(context.getClickLocation(), face, pos));
                    level.setBlock(pos, ((BlockItem) blockItemToPlace).getBlock().getStateForPlacement(blockPlaceContext), 3);
                }
                else {
                    level.setBlock(pos, stateToPlace, 3);
                }
                placed++;
                if (placed == blockCount)
                    break;
            }
            if (!player.isCreative())
                player.getInventory().clearOrCountMatchingItems(itemStack -> itemStack.getItem().equals(blockItemToPlace), placed, player.getInventory());

            if (ToolDamageUtil.damage(tool, blocksToPlace.size(), player, context.getItemInHand())) {
                player.broadcastBreakEvent(source.getSlot(context.getHand()));
            }
            level.playSound(null, mainPos, ((BlockItem)blockItemToPlace).getBlock().defaultBlockState().getSoundType(level, mainPos, player).getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.getCurrentDurability() <= 1
                || !player.isCrouching())
            return InteractionResult.PASS;

        if (!player.getLevel().isClientSide) {
            this.nextMode(tool);
            ((ServerPlayer) player).sendMessage(new TranslatableComponent(getTranslationKey() + ".mode_switch", this.getMode(tool)), ChatType.GAME_INFO, Util.NIL_UUID);
        }
        return InteractionResult.sidedSuccess(player.level.isClientSide);
    }

    enum Mode {
        FILL,
        HORIZONTAL,
        VERTICAL;

        public static final Mode[] values = Mode.values();
    }

    public static List<BlockPos> getBlocksToLay(Level level, BlockPos mainPos, BlockState stateToPlace, boolean requireSameState, Direction face, int expandedLevel, Mode mode) {
        if (expandedLevel > EXPANDED_LEVEL_RANGE.length)
            expandedLevel = EXPANDED_LEVEL_RANGE.length - 1;
        int placeableAmount = EXPANDED_LEVEL_RANGE[expandedLevel];
        List<BlockPos> toCheck = new ArrayList<>();
        toCheck.add(mainPos);

        List<BlockPos> toPlace = new ArrayList<>();
        while (!toCheck.isEmpty() && toPlace.size() < placeableAmount) {
            List<BlockPos> newList = new ArrayList<>();
            for (BlockPos pos : toCheck) {
                switch (mode) {
                    case FILL:
                        for (Direction dir : DIRECTION_CLOCKWISE.get(face)) {
                            BlockPos newPos = pos.relative(dir);
                            if (isValidPosition(level, newPos, face, stateToPlace, requireSameState)
                                    && !toPlace.contains(newPos) && !newList.contains(newPos))
                                newList.add(newPos);
                        }
                        break;
                    case HORIZONTAL:
                    case VERTICAL:
                        for (int i = 0; i < DIRECTION_CLOCKWISE.get(face).size(); i++) {
                            if ((i % 2 == 0 && mode == Mode.HORIZONTAL)
                                    || (i % 2 == 1 && mode == Mode.VERTICAL)) continue;
                            BlockPos newPos = pos.relative(DIRECTION_CLOCKWISE.get(face).get(i));
                            if (isValidPosition(level, newPos, face, stateToPlace, requireSameState)
                                    && !toPlace.contains(newPos) && !newList.contains(newPos))
                                newList.add(newPos);
                        }
                        break;
                }
                toPlace.add(pos);
                if (toPlace.size() >= placeableAmount) {
                    newList.clear();
                    break;
                }
            }
            toCheck = new ArrayList<>(newList);
        }

        return toPlace;
    }

    public static BlockHitResult createBlockHitResult(Vec3 hitPoint, Direction direction, BlockPos pos) {
        int wholeY = (int)hitPoint.y;
        double decimalY = hitPoint.y - wholeY;
        return new BlockHitResult(new Vec3(hitPoint.x, pos.getY() + decimalY, hitPoint.z), direction, pos, false);
    }

    public static boolean isValidPosition(Level level, BlockPos pos, Direction face, BlockState state, boolean requireSameState) {
        return ((requireSameState && level.getBlockState(pos).equals(state))
                    || (!requireSameState && level.getBlockState(pos).canOcclude()))
                && level.getBlockState(pos.relative(face)).getMaterial().isReplaceable()
                && !level.isOutsideBuildHeight(pos.relative(face));
    }

    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !(event.getCamera().getEntity() instanceof LocalPlayer player)
                || Minecraft.getInstance().hitResult == null
                || Minecraft.getInstance().hitResult.getType() != HitResult.Type.BLOCK)
            return;

        ToolStack stack;
        if (player.getMainHandItem().getItem() instanceof ModifiableItem)
            stack = ToolStack.from(player.getMainHandItem());
        else if (player.getOffhandItem().getItem() instanceof ModifiableItem)
            stack = ToolStack.from(player.getOffhandItem());
        else return;

        int construction = stack.getModifierLevel(BCModifiers.CONSTRUCTION.get());
        if (construction == 0)
            return;
        int expandedLevel = stack.getModifierLevel(TinkerModifiers.expanded.get());

        Level level = player.getLevel();
        BlockHitResult blockhitresult = (BlockHitResult) Minecraft.getInstance().hitResult;
        Direction face = blockhitresult.getDirection();
        BlockPos pos = blockhitresult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        ItemStack blockStack = new ItemStack(state.getBlock().asItem());
        if (player.getOffhandItem().getItem() instanceof BlockItem)
            blockStack = player.getOffhandItem();
        if (!level.getBlockState(pos.relative(face)).getMaterial().isReplaceable())
            return;
        List<BlockPos> blocksToPlace = getBlocksToLay(level, pos, state, !(player.getOffhandItem().getItem() instanceof BlockItem), face, expandedLevel, Mode.values[stack.getPersistentData().getInt(MODE)]);
        if (blocksToPlace.size() == 0)
            return;
        VertexConsumer vertexBuilder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.LINES);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(-cam.x, -cam.y, -cam.z);
        for (BlockPos blockPos : blocksToPlace) {
            blockPos = blockPos.relative(face);
            /*event.getPoseStack().pushPose();
            event.getPoseStack().translate(blockPos.getX() + 0.5d, blockPos.getY() + 0.5d, blockPos.getZ() + 0.5d);
            //event.getPoseStack().mulPose(face.getRotation());
            BakedModel model = itemRenderer.getItemModelShaper().getItemModel(blockStack);
            itemRenderer.render(blockStack, ItemTransforms.TransformType.NONE, false, event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), LevelRenderer.getLightColor(level, blockPos), OverlayTexture.NO_WHITE_U, model);
            event.getPoseStack().popPose();*/
            LevelRenderer.renderShape(event.getPoseStack(), vertexBuilder, Shapes.block(), blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1f, 0.2f, 0.2f, 0.667f);
        }
        event.getPoseStack().popPose();
        RenderSystem.disableDepthTest();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    @Override
    public void addInformation(IToolStackView tool, int level, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        int expandedLevel = tool.getModifierLevel(TinkerModifiers.expanded.get());
        tooltip.add(applyStyle(new TranslatableComponent(getTranslationKey() + ".blocks_placed", EXPANDED_LEVEL_RANGE[expandedLevel])));
        tooltip.add(applyStyle(new TranslatableComponent(getTranslationKey() + ".mode", this.getMode(tool))));
    }
}
