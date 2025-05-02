package insane96mcp.buildersconstruct;

import insane96mcp.buildersconstruct.modifiers.AngelBuilderModule;
import insane96mcp.buildersconstruct.modifiers.ConstructionModule;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegisterEvent;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;

public class BCModifiers implements IConditionBuilder {

    public static final ModifierId ANGEL_BUILDER = id("angel_builder");
    public static final ModifierId CONSTRUCTION = id("construction");

    @SubscribeEvent
    void registerSerializers(RegisterEvent event) {
        if (event.getRegistryKey() != Registries.RECIPE_SERIALIZER)
            return;

        ModifierModule.LOADER.register(BuildersConstruct.getResource("angel_builder"), AngelBuilderModule.LOADER);
        ModifierModule.LOADER.register(BuildersConstruct.getResource("construction"), ConstructionModule.LOADER);
    }

    private static ModifierId id(String name) {
        return new ModifierId(BuildersConstruct.MOD_ID, name);
    }
}
