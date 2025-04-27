package insane96mcp.buildersconstruct;

import insane96mcp.buildersconstruct.modifiers.AngelBuilderModule;
import insane96mcp.buildersconstruct.modifiers.ConstructionModifier;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegisterEvent;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class BCModifiers implements IConditionBuilder {
    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(BuildersConstruct.MOD_ID);

    public static final StaticModifier<Modifier> CONSTRUCTION = MODIFIERS.register("construction", ConstructionModifier::new);
    //public static final DynamicModifier ANGEL_BUILDER = MODIFIERS.registerDynamic("angel_builder");
    //public static final StaticModifier<Modifier> ANGEL_BUILDER = MODIFIERS.register("angel_builder", AngelBuilderModifierOld::new);

    @SubscribeEvent
    void registerSerializers(RegisterEvent event) {
        if (event.getRegistryKey() != Registries.RECIPE_SERIALIZER)
            return;

        ModifierModule.LOADER.register(BuildersConstruct.getResource("angel_builder"), AngelBuilderModule.LOADER);
    }
}
