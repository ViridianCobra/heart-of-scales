package net.basilisk.heartofscales.genome;

import java.util.Random;

/** How a child genome is built from two parents. Plain Java so the rules can grow and be tested without Minecraft. */
public final class Inheritance {
    private Inheritance() {}

    /** Subspecies is a coin flip between the parents. Later traits get their own rule here. */
    public static DragonGenome child(DragonGenome a, DragonGenome b, Random random) {
        String subspecies = random.nextBoolean() ? a.subspecies() : b.subspecies();
        return new DragonGenome(subspecies);
    }
}
