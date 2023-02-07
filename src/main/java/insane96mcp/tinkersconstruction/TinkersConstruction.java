package insane96mcp.tinkersconstruction;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod(TinkersConstruction.MOD_ID)
public class TinkersConstruction
{
    public static final String MOD_ID = "tinkersconstruction";
    public static final String RESOURCE_PREFIX = MOD_ID + ":";

    public TinkersConstruction() {
        //ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.COMMON_SPEC);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        TConstructionModifiers.MODIFIERS.register(bus);
        //DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> MaterialisClient::onConstruct);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        if (event.includeServer()) {
            generator.addProvider(new TConstructionModifiers(generator));
        }
    }
}