package org.lushplugins.lushrewards.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.gui.GuiDisplayer;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.reward.module.OldUserDataModule;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsGui;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsModule;
import org.lushplugins.lushrewards.migrator.Migrator;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.pluginupdater.api.updater.Updater;
import org.lushplugins.rewardsapi.api.RewardsAPI;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.exception.CommandErrorException;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Command("rewards")
public class RewardsCommand {

    @Command("rewards")
    @CommandPermission("lushrewards.use")
    public void rewards(BukkitCommandActor actor) {
        LushRewards.getInstance().getConfigManager().checkRefresh();

        Player player = actor.requirePlayer();
        LushRewards.getInstance().getRewardModuleManager().getModules().stream()
            .filter(module -> module instanceof DailyRewardsModule && player.hasPermission("lushrewards.use." + module.getId()))
            .findFirst()
            .ifPresentOrElse(
                module -> new DailyRewardsGui((DailyRewardsModule) module, player).open(),
                () -> ChatColorHandler.sendMessage(player, "&#ff6969The daily rewards module is disabled or you don't have permission")
            );
    }

    @Subcommand("about")
    public void about(CommandSender sender) {
        ChatColorHandler.sendMessage(sender,
            "&#A5B8FE&lLushRewards &#C4B6FE(v" + LushRewards.getInstance().getDescription().getVersion() + ")",
            "&7An extremely configurable, feature rich rewards plugin. ",
            "&7Reward your players each day for logging in and also reward ",
            "&7them for their time spent on the server with playtime rewards!",
            "&r",
            "&7Author: &#f7ba6fLushPlugins",
            "&r",
            "&7Wiki: &#f7ba6fhttps://docs.lushplugins.org/lush-rewards",
            "&7Support: &#f7ba6fhttps://discord.gg/p3duRZsZ2f"
        );
    }

    @Subcommand("claim")
    public void claim(BukkitCommandActor actor, RewardUser user, @Optional RewardModule module) {
        Player player = actor.requirePlayer();

        AtomicInteger rewardsGiven = new AtomicInteger();
        if (module != null) {
            if (this.claim(player, user, module)) {
                rewardsGiven.getAndIncrement();
            }
        } else {
            for (RewardModule enabledModule : LushRewards.getInstance().getEnabledRewardModules()) {
                if (this.claim(player, user, enabledModule)) {
                    rewardsGiven.getAndIncrement();
                }
            }
        }

        if (rewardsGiven.get() == 0) {
            ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("no-rewards-available"));
        }
    }

    /**
     * @return whether a reward has been claimed
     */
    private boolean claim(Player player, RewardUser user, RewardModule module) {
        if (!player.hasPermission("lushrewards.use." + module.getId())) {
            return false;
        }

        if (module.hasClaimableRewards(player)) {
            if (module instanceof OldUserDataModule<?> userDataModule) {
                userDataModule.getOrLoadUserData(player.getUniqueId(), true).thenAccept(userData -> module.claimRewards(player, user));
                return true;
            } else {
                module.claimRewards(player, user);
                return true;
            }
        } else {
            return false;
        }
    }

    @Subcommand("gui")
    @CommandPermission("lushrewards.use")
    public void gui(BukkitCommandActor actor, RewardModule module) {
        Player player = actor.requirePlayer();
        if (!player.hasPermission("lushrewards.use." + module.getId())) {
            // TODO: Work out how to handle this properly - does Lamp have an implementation?
            throw new CommandErrorException("You do not have the required permission for this command");
        }

        if (module instanceof GuiDisplayer guiDisplayer) {
            guiDisplayer.displayGui(player);
        }
    }

    @Subcommand({"import", "migrate"})
    public void migrate(CommandSender sender, Migrator migrator) {
        long startMs = Instant.now().toEpochMilli();
        RewardsAPI.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            try {
                if (migrator.convert()) {
                    ChatColorHandler.sendMessage(sender, "&#b7faa2Successfully imported configuration from &#66b04f'%s' &#b7faa2in &#66b04f%sms"
                        .formatted(migrator.getName(), (Instant.now().toEpochMilli() - startMs)));
                    LushRewards.getInstance().getConfigManager().reloadConfig();
                    ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("reload"));
                } else {
                    ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'%s' &#ff6969in &#d13636%sms"
                        .formatted(migrator.getName(), (Instant.now().toEpochMilli() - startMs)));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'%s' &#ff6969in &#d13636%sms"
                    .formatted(migrator.getName(), (Instant.now().toEpochMilli() - startMs)));
            }
        });
    }

    @Subcommand("reload")
    @CommandPermission("lushrewards.reload")
    public String reload() {
        LushRewards.getInstance().getConfigManager().reloadConfig();
        return LushRewards.getInstance().getConfigManager().getMessage("reload");
    }

    @Subcommand("update")
    @CommandPermission("lushrewards.update")
    public CompletableFuture<String> update() {
        Updater updater = LushRewards.getInstance().getUpdater();
        if (updater.isAlreadyDownloaded() || !updater.isUpdateAvailable()) {
            return CompletableFuture.completedFuture("&#ff6969It looks like there is no new update available!");
        }

        return updater.attemptDownload().thenApply(success -> {
            if (success) {
                return "&#b7faa2Successfully updated LushRewards, restart the server to apply changes!";
            } else {
                return "&#ff6969Failed to update LushRewards!";
            }
        });
    }

    @Subcommand("version")
    @CommandPermission("lushrewards.version")
    public String version(BukkitCommandActor actor) {
        return "&#a8e1ffYou are currently running &#58b1e0LushRewards &#a8e1ffversion &#58b1e0" + LushRewards.getInstance().getDescription().getVersion();
    }
}
