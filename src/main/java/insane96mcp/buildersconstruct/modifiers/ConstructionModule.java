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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.tconstruct.library.json.math.FormulaLoadable;
import slimeknights.tconstruct.library.json.math.ModifierFormula;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.*;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = BuildersConstruct.MOD_ID)
public record ConstructionModule(ModifierFormula blocksPlaced, ModifierCondition<IToolStackView> condition) implements BlockInteractionModifierHook, GeneralInteractionModifierHook, TooltipModifierHook, AttributesModifierHook, ModifierModule, ModifierCondition.ConditionalModule<IToolStackView> {

    public static final RegistryObject<Attribute> BLOCKS_PLACED = BuildersConstruct.ATTRIBUTES.register("blocks_placed", () -> new RangedAttribute("attribute.name.buildersconstruct:blocks_placed", 0d, 0d, Double.MAX_VALUE));

    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ConstructionModule>defaultHooks(ModifierHooks.GENERAL_INTERACT, ModifierHooks.BLOCK_INTERACT, ModifierHooks.TOOLTIP);
    /** Setup for the formula */
    private static final FormulaLoadable FORMULA = new FormulaLoadable(ModifierFormula.FallbackFormula.BOOST, "level", "expanded_level");
    public static final RecordLoadable<ConstructionModule> LOADER = RecordLoadable.create(
            FORMULA.requiredField("blocks_placed", ConstructionModule::blocksPlaced),
            slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.TOOL_FIELD,
            ConstructionModule::new);

    private static final ResourceLocation MODE = ResourceLocation.fromNamespaceAndPath(BuildersConstruct.MOD_ID, "construction_mode");

    private static Map<Direction, List<Direction>> DIRECTION_CLOCKWISE;

    @Override
    public RecordLoadable<? extends GenericLoaderRegistry.IHaveLoader> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        DIRECTION_CLOCKWISE = new EnumMap<>(Direction.class);
        DIRECTION_CLOCKWISE.put(Direction.UP, Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.DOWN, Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.NORTH, Arrays.asList(Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.SOUTH, Arrays.asList(Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST));
        DIRECTION_CLOCKWISE.put(Direction.EAST, Arrays.asList(Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH));
        DIRECTION_CLOCKWISE.put(Direction.WEST, Arrays.asList(Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH));

        return DEFAULT_HOOKS;
    }

    @Override
    public Integer getPriority() {
        // run multipliers a bit later
        return 101;
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
    public InteractionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        if (tool.getCurrentDurability() < 1
                || source != InteractionSource.RIGHT_CLICK
                //|| !tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)
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
                if (stateToPlace == null)
                    return InteractionResult.PASS;
                offHandBlockPlacing = true;
            }
            else {
                blockItemToPlace = stateToPlace.getBlock().asItem();
            }
            if (stateToPlace.hasBlockEntity())
                return InteractionResult.PASS;
            BlockPos mainPos = context.getClickedPos();
            //Place blocks only if the block on the face is air
            if (!level.getBlockState(mainPos.relative(face)).canBeReplaced())
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
            List<BlockPos> blocksToPlace = getBlocksToLay(level, mainPos, stateToPlace, !offHandBlockPlacing, face, player.getDirection(), player, this.getMode(tool));
            int placed = 0;
            for (BlockPos pos : blocksToPlace) {
                pos = pos.relative(face);
                if (offHandBlockPlacing) {
                    BlockPlaceContext blockPlaceContext = new BlockPlaceContext(player, context.getHand(), player.getItemInHand(context.getHand()), createBlockHitResult(context.getClickLocation(), face, pos));
                    BlockState blockState = ((BlockItem) blockItemToPlace).getBlock().getStateForPlacement(blockPlaceContext);
                    if (blockState == null)
                        continue;
                    level.setBlock(pos, blockState, 3);
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
            level.playSound(null, mainPos, ((BlockItem) blockItemToPlace).getBlock().defaultBlockState().getSoundType(level, mainPos, player).getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.getCurrentDurability() < 1
                || !player.isCrouching()
                //|| !tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)
                || source != InteractionSource.RIGHT_CLICK)
            return InteractionResult.PASS;

        this.nextMode(tool);
        player.displayClientMessage(modifier.getModifier().applyStyle(Component.translatable(modifier.getModifier().getTranslationKey() + ".mode_switch", this.getMode(tool))), true);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
        consumer.accept(BLOCKS_PLACED.get(),
                new AttributeModifier(UUID.fromString("982c9f4c-4763-4b91-9483-66df04e881c7"),
                        "construction_modifier",
                        placeableAmount(modifier.getLevel(), tool.getModifierLevel(TinkerModifiers.expanded.get())),
                        AttributeModifier.Operation.ADDITION));
    }

    public enum Mode {
        FILL,
        HORIZONTAL,
        VERTICAL;

        public static final Mode[] values = Mode.values();
    }

    private int placeableAmount(int lvl, int expandedLevel) {
        return (int) blocksPlaced.apply(lvl, expandedLevel);
    }

    public static List<BlockPos> getBlocksToLay(Level level, BlockPos mainPos, BlockState stateToPlace, boolean requireSameState, Direction face, Direction playerFacing, Player player, Mode mode) {
        int placeableAmount = (int) player.getAttributeValue(BLOCKS_PLACED.get());
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
                        if (face.getAxis() == Direction.Axis.Y) {
                            BlockPos newPos;
                            if (mode == Mode.HORIZONTAL) {
                                newPos = pos.relative(playerFacing.getClockWise());
                                if (isValidPosition(level, newPos, face, stateToPlace, requireSameState)
                                        && !toPlace.contains(newPos) && !newList.contains(newPos))
                                    newList.add(newPos);
                                newPos = pos.relative(playerFacing.getCounterClockWise());
                            }
                            else {
                                newPos = pos.relative(playerFacing);
                                if (isValidPosition(level, newPos, face, stateToPlace, requireSameState)
                                        && !toPlace.contains(newPos) && !newList.contains(newPos))
                                    newList.add(newPos);
                                newPos = pos.relative(playerFacing.getOpposite());
                            }
                            if (isValidPosition(level, newPos, face, stateToPlace, requireSameState)
                                    && !toPlace.contains(newPos) && !newList.contains(newPos))
                                newList.add(newPos);
                        }
                        else {
                            for (int i = 0; i < DIRECTION_CLOCKWISE.get(face).size(); i++) {
                                if ((i % 2 == 0 && mode == Mode.HORIZONTAL)
                                        || (i % 2 == 1 && mode == Mode.VERTICAL)) continue;
                                BlockPos newPos = pos.relative(DIRECTION_CLOCKWISE.get(face).get(i));
                                if (isValidPosition(level, newPos, face, stateToPlace, requireSameState)
                                        && !toPlace.contains(newPos) && !newList.contains(newPos))
                                    newList.add(newPos);
                            }
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
                && level.getBlockState(pos.relative(face)).canBeReplaced()
                && !level.isOutsideBuildHeight(pos.relative(face));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !(event.getCamera().getEntity() instanceof LocalPlayer player)
                || Minecraft.getInstance().hitResult == null
                || Minecraft.getInstance().hitResult.getType() != HitResult.Type.BLOCK)
            return;

        ItemStack stack;
        if (player.getMainHandItem().getItem() instanceof ModifiableItem)
            stack = player.getMainHandItem();
        else if (player.getOffhandItem().getItem() instanceof ModifiableItem)
            stack = player.getOffhandItem();
        else return;

        int construction = ModifierUtil.getModifierLevel(stack, BCModifiers.CONSTRUCTION);
        if (construction == 0)
            return;
        int expandedLevel = ModifierUtil.getModifierLevel(stack, TinkerModifiers.expanded.get().getId());

        Level level = player.level();
        BlockHitResult blockhitresult = (BlockHitResult) Minecraft.getInstance().hitResult;
        Direction face = blockhitresult.getDirection();
        BlockPos pos = blockhitresult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (player.getOffhandItem().getItem() instanceof BlockItem) {
            Item blockItemToPlace = player.getOffhandItem().getItem();
            state = ((BlockItem) blockItemToPlace).getBlock().defaultBlockState();
        }
        if (state.hasBlockEntity())
            return;
        /*ItemStack blockStack = new ItemStack(state.getBlock().asItem());
        if (player.getOffhandItem().getItem() instanceof BlockItem)
            blockStack = player.getOffhandItem();*/
        if (!level.getBlockState(pos.relative(face)).canBeReplaced())
            return;
        List<BlockPos> blocksToPlace = getBlocksToLay(level, pos, state, !(player.getOffhandItem().getItem() instanceof BlockItem), face, player.getDirection(), player, Mode.values[ToolStack.from(stack).getPersistentData().getInt(MODE)]);
        if (blocksToPlace.isEmpty())
            return;
        VertexConsumer vertexBuilder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.LINES);
        //ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(-cam.x, -cam.y, -cam.z);
        for (BlockPos blockPos : blocksToPlace) {
            blockPos = blockPos.relative(face);
            LevelRenderer.renderShape(event.getPoseStack(), vertexBuilder, Shapes.block(), blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1f, 0.2f, 0.2f, 0.5f);
        }
        event.getPoseStack().popPose();
        RenderSystem.disableDepthTest();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        tooltip.add(modifier.getModifier().applyStyle(Component.translatable(modifier.getModifier().getTranslationKey() + ".blocks_placed", placeableAmount(modifier.getLevel(), tool.getModifierLevel(TinkerModifiers.expanded.get())))));
        tooltip.add(modifier.getModifier().applyStyle(Component.translatable(modifier.getModifier().getTranslationKey() + ".mode", this.getMode(tool))));
    }
}
