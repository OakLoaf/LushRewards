package org.lushplugins.lushrewards.placeholder;

import org.bukkit.entity.Player;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.placeholderhandler.annotation.Placeholder;
import org.lushplugins.placeholderhandler.annotation.SubPlaceholder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Placeholder("lushrewards")
public class Placeholders {
    private static LocalDateTime MIDNIGHT = LocalDate.now().plusDays(1).atStartOfDay();

    @SubPlaceholder("countdown")
    public String countdown() {
        LocalDateTime now = LocalDateTime.now();

        long secondsUntil = now.until(MIDNIGHT, ChronoUnit.SECONDS);

        if (secondsUntil < 0) {
            MIDNIGHT = LocalDate.now().plusDays(1).atStartOfDay();
            secondsUntil = now.until(MIDNIGHT, ChronoUnit.SECONDS);
        }

        long hours = secondsUntil / 3600;
        long minutes = (secondsUntil % 3600) / 60;
        long seconds = secondsUntil % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @SubPlaceholder("global_playtime")
    public String globalPlaytime(Player player) {
        Module playtimeTracker = LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).orElse(null);
        if (playtimeTracker instanceof PlaytimeTrackerModule playtimeTrackerModule) {
            return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getGlobalPlaytime());
        } else {
            return null;
        }
    }

    @SubPlaceholder("session_playtime")
    public String sessionPlaytime(Player player) {
        Module playtimeTracker = LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).orElse(null);
        if (playtimeTracker instanceof PlaytimeTrackerModule playtimeTrackerModule) {
            return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getSessionPlaytime());
        } else {
            return null;
        }
    }

    @SubPlaceholder("total_session_playtime")
    public String totalSessionPlaytime(Player player) {
        Module playtimeTracker = LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).orElse(null);
        if (playtimeTracker instanceof PlaytimeTrackerModule playtimeTrackerModule) {
            return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getSessionPlaytime());
        } else {
            return null;
        }
    }
}
