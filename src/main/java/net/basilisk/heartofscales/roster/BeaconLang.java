package net.basilisk.heartofscales.roster;

import java.util.List;

/** Translation keys for the beacon screen. Free of Minecraft classes so tests can check them against en_us.json. */
public final class BeaconLang {
    public static final String TITLE = "container.heart_of_scales.dragon_beacon";
    public static final String EMPTY = "beacon.heart_of_scales.empty";
    public static final String TAMED = "beacon.heart_of_scales.tamed";
    public static final String TAMED_UNKNOWN = "beacon.heart_of_scales.tamed_unknown";
    public static final String YOU = "beacon.heart_of_scales.you";
    public static final String UNKNOWN_PLAYER = "beacon.heart_of_scales.unknown_player";
    public static final String DIED_AT = "beacon.heart_of_scales.died_at";
    public static final String REMOVED_AT = "beacon.heart_of_scales.removed_at";
    /** Paths of the minecraft dimensions that get a friendly name; any other dimension shows its id. */
    public static final List<String> VANILLA_DIMENSIONS = List.of("overworld", "the_nether", "the_end");

    public static String dimension(String vanillaPath) {
        return "beacon.heart_of_scales.dimension." + vanillaPath;
    }

    private BeaconLang() {}
}
