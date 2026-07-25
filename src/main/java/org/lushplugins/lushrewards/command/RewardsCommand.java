package org.lushplugins.lushrewards.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.gui.GuiDisplayer;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.reward.module.StoresUserData;
import org.lushplugins.lushrewards.migrator.Migrator;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.pluginupdater.api.updater.Updater;
import org.lushplugins.rewardsapi.api.RewardsAPI;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.node.ExecutionContext;

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
        String defaultRewardGui = LushRewards.getInstance().getConfigManager().getDefaultRewardGui();
        RewardModule module = LushRewards.getInstance().getRewardModuleManager().getModule(defaultRewardGui);
        if (module instanceof GuiDisplayer displayer && player.hasPermission("lushrewards.use.%s" .formatted(module.getId()))) {
            displayer.displayGui(player);
        } else {
            ChatColorHandler.sendMessage(player, "&#ff6969The '%s' module is disabled, doesn't have a gui or you don't have permission"
                .formatted(defaultRewardGui));
        }
    }

    @Subcommand("about")
    @CommandPermission("lushrewards.about")
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
    @CommandPermission("lushrewards.use")
    public void claim(BukkitCommandActor actor, RewardUser user, @Optional RewardModule module) {
        Player player = actor.requirePlayer();

        AtomicInteger rewardsGiven = new AtomicInteger();
        if (module != null) {
            if (this.claim(player, user, module)) {
                rewardsGiven.getAndIncrement();
            }
        } else {
            for (RewardModule enabledModule : LushRewards.getInstance().getRewardModuleManager().getModules()) {
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
            StoresUserData userDataAnnotation = module.getClass().getAnnotation(StoresUserData.class);
            if (userDataAnnotation != null) {
                user.getModuleData(module.getId(), userDataAnnotation.value()).thenAccept(userData -> module.claimRewards(player, user));
            } else {
                module.claimRewards(player, user);
            }

            return true;
        } else {
            return false;
        }
    }

    @Subcommand("gui")
    @CommandPermission("lushrewards.use")
    public void gui(ExecutionContext<BukkitCommandActor> context, RewardModule module) {
        Player player = context.actor().requirePlayer();
        if (!player.hasPermission("lushrewards.use." + module.getId())) {
            throw new NoPermissionException(context.command());
        }

        if (module instanceof GuiDisplayer guiDisplayer) {
            guiDisplayer.displayGui(player);
        }
    }

    @Subcommand({"import", "migrate"})
    @CommandPermission("lushrewards.import")
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
        LushRewards.getInstance().getRewardModuleManager().reloadModules();

        return LushRewards.getInstance().getConfigManager().getMessage("reload");
    }

    @Subcommand("update")
    @CommandPermission("lushrewards.update")
    public CompletableFuture<String> update() {
        Updater<?> updater = LushRewards.getInstance().getUpdater();
        if (updater == null) {
            return CompletableFuture.completedFuture("&#ff6969The updater is currently disabled");
        }

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
