package insane96mcp.buildersconstruct;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

@Mod(BuildersConstruct.MOD_ID)
public class BuildersConstruct
{
    public static final String MOD_ID = "buildersconstruct";

    public static final List<DeferredRegister<?>> REGISTRIES = new ArrayList<>();
    public static final DeferredRegister<Attribute> ATTRIBUTES = createRegistry(ForgeRegistries.ATTRIBUTES);

    public BuildersConstruct(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        bus.register(new BCModifiers());
        REGISTRIES.forEach(register -> register.register(bus));

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation getResource(String id) {
        return ResourceLocation.fromNamespaceAndPath(BuildersConstruct.MOD_ID, id);
    }

    static <R> DeferredRegister<R> createRegistry(IForgeRegistry<R> reg) {
        DeferredRegister<R> register = DeferredRegister.create(reg, MOD_ID);
        REGISTRIES.add(register);
        return register;
    }
}