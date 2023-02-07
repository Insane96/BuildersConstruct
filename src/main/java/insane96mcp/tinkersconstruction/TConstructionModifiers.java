package insane96mcp.tinkersconstruction;

import insane96mcp.tinkersconstruction.modifiers.ConstructionModifier;
import net.minecraft.data.DataGenerator;
import slimeknights.tconstruct.library.data.tinkering.AbstractModifierProvider;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class TConstructionModifiers extends AbstractModifierProvider {
    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(TinkersConstruction.MOD_ID);

    public static final StaticModifier<Modifier> voidingModifier = MODIFIERS.register("construction", ConstructionModifier::new);

    public TConstructionModifiers(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void addModifiers() {
    }

    @Override
    public String getName() {
        return "Tinkers' Construction Modifiers";
    }
}
