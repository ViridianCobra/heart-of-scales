package net.basilisk.heartofscales.client.screen;

import net.basilisk.heartofscales.roster.BeaconLang;
import net.basilisk.heartofscales.roster.BeaconRow;
import net.basilisk.heartofscales.roster.RosterTime;
import net.basilisk.heartofscales.roster.RowStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns a beacon row into the text the screen shows. */
final class BeaconRowText {
    private static final Component SEPARATOR = Component.literal(" · ");
    private static final Component ELLIPSIS = Component.literal("…");
    private static final int TOOLTIP_WIDTH = 220;

    static Component tamed(BeaconRow row) {
        if (row.tamedAt() == RosterTime.UNKNOWN) return Component.translatable(BeaconLang.TAMED_UNKNOWN);
        return Component.translatable(BeaconLang.TAMED, time(row.tamedAt()));
    }

    /** Owner, status, then whatever helps find the dragon. {@code here} is the viewer's dimension. */
    static Component details(BeaconRow row, ResourceKey<Level> here) {
        RowStatus status = row.status();
        MutableComponent line = Component.empty().append(row.owner()).append(SEPARATOR).append(status(row));
        if (status == RowStatus.HOME || status == RowStatus.AWAY) line.append(SEPARATOR).append(row.command().displayName());
        if (status != RowStatus.HOME) line.append(SEPARATOR).append(coordinates(row.pos()));
        if (status.showsDimension(!row.pos().dimension().equals(here))) line.append(" ").append(dimension(row.pos()));
        return line;
    }

    /** The full details, wrapped, plus when the dragon died or was removed. */
    static List<FormattedCharSequence> tooltip(Font font, BeaconRow row, ResourceKey<Level> here) {
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(details(row, here), TOOLTIP_WIDTH));
        if (row.status() == RowStatus.DIED) {
            lines.add(Component.translatable(BeaconLang.DIED_AT, time(row.endedAt())).getVisualOrderText());
        } else if (row.status() == RowStatus.REMOVED) {
            lines.add(Component.translatable(BeaconLang.REMOVED_AT, time(row.endedAt())).getVisualOrderText());
        }
        return lines;
    }

    /** Cuts text that would run past maxWidth and ends it with an ellipsis. */
    static FormattedCharSequence fit(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text.getVisualOrderText();
        FormattedText cut = font.substrByWidth(text, maxWidth - font.width(ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, ELLIPSIS));
    }

    private static Component status(BeaconRow row) {
        if (row.status() == RowStatus.DIED) {
            Component message = row.deathMessage() != null ? row.deathMessage() : Component.empty();
            return Component.translatable(row.status().translationKey(), message);
        }
        return Component.translatable(row.status().translationKey());
    }

    private static String time(long epochMillis) {
        return RosterTime.format(epochMillis, ZoneId.systemDefault(), Locale.getDefault());
    }

    private static Component coordinates(GlobalPos pos) {
        BlockPos at = pos.pos();
        return Component.literal(at.getX() + " " + at.getY() + " " + at.getZ());
    }

    private static Component dimension(GlobalPos pos) {
        ResourceLocation id = pos.dimension().location();
        boolean vanilla = id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                && BeaconLang.VANILLA_DIMENSIONS.contains(id.getPath());
        return vanilla ? Component.translatable(BeaconLang.dimension(id.getPath())) : Component.literal(id.toString());
    }

    private BeaconRowText() {}
}
