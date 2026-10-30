package hawi.bucket.edenmcregions;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.*;

/** Read-only layout matching vanilla's 15-row sidebar, including team-specific objectives. */
public record SidebarLayout(
        boolean present,
        DisplaySlot slot,
        Objective objective,
        Component title,
        int titleWidth,
        List<Row> rows,
        int width,
        int bottom,
        int headerTop,
        int headerBottom
) {
    public record Row(Component name, Component score, int nameWidth, int scoreWidth) {}

    public static SidebarLayout measure(int screenHeight) {
        Minecraft client = Minecraft.getInstance();
        Scoreboard scoreboard = client.level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(client.player.getScoreboardName());
        DisplaySlot slot = team == null ? null : HudPlatform.teamSlot(team);
        Objective objective = slot == null ? null : scoreboard.getDisplayObjective(slot);
        if (objective == null) {
            slot = DisplaySlot.SIDEBAR;
            objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        }
        if (objective == null) {
            return new SidebarLayout(false, null, null, null, 0, List.of(), 0, screenHeight / 2, screenHeight / 2 - 10, screenHeight / 2);
        }
        var format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        var scores = scoreboard.listPlayerScores(objective).stream().filter(s -> !s.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
                        .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
                .limit(15).toList();
        Component title = objective.getDisplayName();
        int titleWidth = client.font.width(title);
        int maxWidth = titleWidth;
        List<Row> rows = new ArrayList<>(scores.size());
        for (var score : scores) {
            Component name = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(score.owner()), score.ownerName());
            Component scoreComp = score.formatValue(format);
            int nameWidth = client.font.width(name);
            int scoreWidth = client.font.width(scoreComp);
            maxWidth = Math.max(maxWidth, nameWidth + (scoreWidth > 0 ? client.font.width(": ") + scoreWidth : 0));
            rows.add(new Row(name, scoreComp, nameWidth, scoreWidth));
        }
        int bottom = screenHeight / 2 + (scores.size() * 9) / 3;
        int headerY = bottom - scores.size() * 9 - 9;
        return new SidebarLayout(true, slot, objective, title, titleWidth, List.copyOf(rows), maxWidth, bottom, headerY - 1, headerY + 8);
    }
}



