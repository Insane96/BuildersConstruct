package insane96mcp.buildersconstruct;

import insane96mcp.buildersconstruct.modifiers.AngelBuilderModifier;
import insane96mcp.buildersconstruct.modifiers.ConstructionModifier;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import slimeknights.tconstruct.library.data.tinkering.AbstractModifierProvider;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class BCModifiers extends AbstractModifierProvider implements IConditionBuilder {
    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(BuildersConstruct.MOD_ID);

    public static final StaticModifier<Modifier> CONSTRUCTION = MODIFIERS.register("construction", ConstructionModifier::new);
    public static final StaticModifier<Modifier> ANGEL_BUILDER = MODIFIERS.register("angel_builder", AngelBuilderModifier::new);

    public BCModifiers(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void addModifiers() {

    }

    @Override
    public String getName() {
        return "Builders Constructs Modifiers";
    }
}
