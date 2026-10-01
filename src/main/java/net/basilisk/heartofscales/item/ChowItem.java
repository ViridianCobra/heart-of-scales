package net.basilisk.heartofscales.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A subspecies' favourite food, served in a bowl. */
public class ChowItem extends Item {
    public ChowItem(Properties properties) {
        super(properties);
    }

    /** Hands the empty bowl back after a dragon eats, into the hand if that was the last chow, like finishing a stew. */
    public static void returnBowl(Player player, InteractionHand hand) {
        if (player.getAbilities().instabuild) return;
        ItemStack bowl = new ItemStack(Items.BOWL);
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, bowl);
        } else if (!player.getInventory().add(bowl)) {
            player.drop(bowl, false);
        }
    }
}
