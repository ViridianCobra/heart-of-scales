package net.basilisk.heartofscales.entity;

import net.basilisk.heartofscales.HeartOfScales;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Map;

/**
 * One body plan: the model files a dragon is drawn with and how it is sized, ridden and animated.
 * Heights and reach are in blocks at scale 1 and are multiplied by {@link DragonEntity#getBodyScale()}.
 *
 * @param asset       name of the geo, texture and animation files under {@code assets/heart_of_scales}
 * @param seatHeight  rider seat above the feet; a sitting rider looks right a little below the saddle's top
 * @param pivotHeight height the body pitches and rolls about in flight, roughly the body's centre
 * @param reach       how far the model reaches past the hitbox, for culling; 0 culls on the hitbox alone
 * @param tinted      whether the subspecies egg tint is applied, for a greyscale texture
 * @param turnsHead   whether GeckoLib turns the {@code head} bone to look, which overwrites its keyframes
 */
public record DragonBody(String asset, float scale, double seatHeight, double pivotHeight, float reach,
                         boolean tinted, boolean turnsHead, Animations animations) {
    // No swim animation exists yet, so swimming reuses fly
    public static final DragonBody PLACEHOLDER = new DragonBody("dragon", 1.0f, 1.1, 0.9, 0.0f, true, true,
            Animations.of("misc.idle", "move.walk", "misc.sit", "misc.fly", "misc.glide", "misc.fly", "misc.idle", "misc.glide"));
    public static final DragonBody FOREST = new DragonBody("forest-dragon", 1.0f, 1.3, 0.75, 7.5f, false, false,
            Animations.of("idle", "walk", "sit", "test_flight", "glide", "test_flight", "jump_charge", "jump"));

    private static final Map<String, DragonBody> BY_SUBSPECIES = Map.of(HeartOfScales.MOD_ID + ":forest", FOREST);

    /** The body for a subspecies id; anything without its own model uses the placeholder. */
    public static DragonBody forSubspecies(String subspeciesId) {
        return BY_SUBSPECIES.getOrDefault(subspeciesId, PLACEHOLDER);
    }

    /** Built with thenPlay, so each animation keeps the loop setting from its file, such as a sit that holds. */
    public record Animations(RawAnimation idle, RawAnimation walk, RawAnimation sit, RawAnimation fly,
                             RawAnimation glide, RawAnimation swim, RawAnimation jumpCharge, RawAnimation jump) {
        static Animations of(String idle, String walk, String sit, String fly, String glide, String swim,
                             String jumpCharge, String jump) {
            return new Animations(play(idle), play(walk), play(sit), play(fly), play(glide), play(swim),
                    play(jumpCharge), play(jump));
        }

        private static RawAnimation play(String name) {
            return RawAnimation.begin().thenPlay(name);
        }
    }
}
