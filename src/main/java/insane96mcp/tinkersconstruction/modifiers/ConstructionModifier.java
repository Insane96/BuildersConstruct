package insane96mcp.tinkersconstruction.modifiers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.tools.definition.module.ToolModuleHooks;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class ConstructionModifier extends Modifier implements BlockInteractionModifierHook {

    @Override
    public Component getDisplayName(IToolStackView tool, int level) {
        return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, level));
    }

    @Override
    public InteractionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        if (tool.getCurrentDurability() > 1 && tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            Player player = context.getPlayer();
            if (!context.getLevel().isClientSide) {
                Level world = context.getLevel();
                Direction face = context.getClickedFace();
                BlockState stateClicked = context.getLevel().getBlockState(context.getClickedPos());
                BlockPos pos = context.getClickedPos().relative(face);
                context.getLevel().setBlock(pos, stateClicked, 3);
                if (ToolDamageUtil.damage(tool, 10, player, context.getItemInHand()) && player != null) {
                    player.broadcastBreakEvent(source.getSlot(context.getHand()));
                }
                world.playSound(null, pos, world.getBlockState(pos).getSoundType(world, pos, player).getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
