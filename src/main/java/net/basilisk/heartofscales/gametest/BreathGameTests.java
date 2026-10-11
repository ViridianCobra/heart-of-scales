package net.basilisk.heartofscales.gametest;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.entity.breath.BreathAim;
import net.basilisk.heartofscales.entity.breath.Breaths;
import net.basilisk.heartofscales.registry.ModEntities;
import net.basilisk.heartofscales.species.stats.BreathStats;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Breath on a real server. A dragon stands at the south end of a stone floor facing south (+Z) with a cow in front of it;
 * each test fires one breath at the cow. Run with {@code gradlew runGameTestServer}.
 */
@GameTestHolder(HeartOfScales.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BreathGameTests {
    private static final String ARENA = "breath-arena";
    // Entity searches miss mobs spawned in the test's first ticks, so everything fires once the arena has settled
    private static final int SETTLE_TICKS = 20;
    // The arena is placed one block above the test's origin, so its stone floor is at y 1 and mobs stand at y 2
    private static final Vec3 DRAGON_POS = new Vec3(4.5, 2.0, 2.5);
    // Dragon is 1.5 wide and a cow 0.9, so this puts the cow's back against the dragon's nose
    private static final Vec3 AGAINST_THE_NOSE = new Vec3(4.5, 2.0, 3.7);
    private static final Vec3 AHEAD = new Vec3(4.5, 2.0, 8.5);

    private BreathGameTests() {}

    @GameTest(template = ARENA)
    public static void rockHurtsAMobAhead(GameTestHelper helper) {
        fireAndExpectHurt(helper, AHEAD, true);
    }

    @GameTest(template = ARENA)
    public static void rockHurtsAMobAgainstTheNose(GameTestHelper helper) {
        fireAndExpectHurt(helper, AGAINST_THE_NOSE, true);
    }

    @GameTest(template = ARENA)
    public static void streamHurtsAMobAhead(GameTestHelper helper) {
        fireAndExpectHurt(helper, AHEAD, false);
    }

    @GameTest(template = ARENA)
    public static void streamHurtsAMobAgainstTheNose(GameTestHelper helper) {
        fireAndExpectHurt(helper, AGAINST_THE_NOSE, false);
    }

    @GameTest(template = ARENA)
    public static void streamDoesNotPushWhatItCannotHurt(GameTestHelper helper) {
        DragonEntity dragon = dragon(helper);
        Cow cow = cow(helper, AHEAD);
        cow.setInvulnerable(true);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            Breaths.AIR.tickServer(dragon, BreathStats.DEFAULT, aimAtCow(dragon, cow), 0);
            helper.assertTrue(cow.getDeltaMovement().y <= 0.0, "an invulnerable cow was pushed upwards");
            helper.succeed();
        });
    }

    private static void fireAndExpectHurt(GameTestHelper helper, Vec3 cowPos, boolean rock) {
        DragonEntity dragon = dragon(helper);
        Cow cow = cow(helper, cowPos);
        float health = cow.getHealth();
        helper.runAfterDelay(SETTLE_TICKS, () ->
                (rock ? Breaths.ROCK : Breaths.AIR).tickServer(dragon, BreathStats.DEFAULT, aimAtCow(dragon, cow), 0));
        helper.succeedWhen(() -> helper.assertTrue(cow.getHealth() < health, "the cow was not hurt"));
    }

    private static DragonEntity dragon(GameTestHelper helper) {
        DragonEntity dragon = helper.spawn(ModEntities.DRAGON.get(), DRAGON_POS);
        dragon.setNoAi(true);
        dragon.setYRot(0.0f);
        dragon.setYBodyRot(0.0f);
        dragon.setYHeadRot(0.0f);
        return dragon;
    }

    private static Cow cow(GameTestHelper helper, Vec3 pos) {
        Cow cow = helper.spawn(EntityType.COW, pos);
        cow.setNoAi(true);
        return cow;
    }

    private static BreathAim aimAtCow(DragonEntity dragon, Cow cow) {
        return dragon.getBreath().aimAt(cow.getBoundingBox().getCenter());
    }
}
