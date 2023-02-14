package insane96mcp.buildersconstruct;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod(BuildersConstruct.MOD_ID)
public class BuildersConstruct
{
    public static final String MOD_ID = "buildersconstruct";
    public static final String RESOURCE_PREFIX = MOD_ID + ":";

    public BuildersConstruct() {
        //ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.COMMON_SPEC);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BCModifiers.MODIFIERS.register(bus);
        //DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> MaterialisClient::onConstruct);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        if (event.includeServer()) {
            generator.addProvider(new BCModifiers(generator));
        }
    }
}