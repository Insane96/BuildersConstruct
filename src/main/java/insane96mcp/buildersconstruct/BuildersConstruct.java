package insane96mcp.buildersconstruct;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BuildersConstruct.MOD_ID)
public class BuildersConstruct
{
    public static final String MOD_ID = "buildersconstruct";
    public static final String RESOURCE_PREFIX = MOD_ID + ":";

    public BuildersConstruct(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        BCModifiers.MODIFIERS.register(bus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        boolean server = event.includeServer();
        if (event.includeServer()) {
            generator.addProvider(server, new BCModifiers(packOutput));
        }
    }
}