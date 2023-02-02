package sunsetsatellite.nocrashes;

import turniplabs.halplibe.helper.*;
import turniplabs.halplibe.mixin.accessors.BlockAccessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.src.*;
import net.minecraft.src.material.ArmorMaterial;
import net.minecraft.src.material.ToolMaterial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;


public class NoCrashes implements ModInitializer {
    public static final String MOD_ID = "nocrashes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("NoCrashes initialized.");
    }
}
