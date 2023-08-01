package sunsetsatellite.nocrashes;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NoCrashes implements ModInitializer {
    public static final String MOD_ID = "nocrashes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("NoCrashes initialized.");
    }

}
