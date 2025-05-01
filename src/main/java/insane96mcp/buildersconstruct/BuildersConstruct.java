package insane96mcp.buildersconstruct;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BuildersConstruct.MOD_ID)
public class BuildersConstruct
{
    public static final String MOD_ID = "buildersconstruct";

    public BuildersConstruct(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        bus.register(new BCModifiers());

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation getResource(String id) {
        return ResourceLocation.fromNamespaceAndPath(BuildersConstruct.MOD_ID, id);
    }
}