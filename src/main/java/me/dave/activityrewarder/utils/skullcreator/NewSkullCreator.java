package me.dave.activityrewarder.utils.skullcreator;

import me.dave.activityrewarder.ActivityRewarder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewSkullCreator implements SkullCreator {
    private static final Pattern skinJsonPattern = Pattern.compile("\"?skin\"?:\\{\"?url\"?:\"?(.[^{}\"]+)\"?}", Pattern.CASE_INSENSITIVE);

    public ItemStack getCustomSkull(String texture) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            mutateItemMeta(meta, texture);
            item.setItemMeta(meta);
            return item;
        } else {
            return null;
        }
    }

    public void mutateItemMeta(SkullMeta meta, String b64) {
        PlayerProfile ownerProfile = meta.getOwnerProfile() != null ? meta.getOwnerProfile() : makeProfile(b64);
        meta.setOwnerProfile(ownerProfile);
    }

    @Nullable
    public String getB64(ItemStack itemStack) {
        try {
            if (itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getOwnerProfile() != null) {
                URL skinUrl = skullMeta.getOwnerProfile().getTextures().getSkin();
                return skinUrl != null ? getBase64FromUrl(skinUrl) : null;
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    public String getTexture(Player player) {
        URL skinUrl = player.getPlayerProfile().getTextures().getSkin();
        return skinUrl != null ? getBase64FromUrl(skinUrl) : null;
    }

    private PlayerProfile makeProfile(String b64) {
        UUID id = null;
        try {
            id = new UUID(b64.substring(b64.length() - 20).hashCode(), b64.substring(b64.length() - 10).hashCode());
        } catch (StringIndexOutOfBoundsException ex) {
            if (b64.length() == 0) {
                ActivityRewarder.getInstance().getLogger().warning("Missing base64 texture found - check your config");
            } else {
                ActivityRewarder.getInstance().getLogger().warning("Invalid base64 texture (" + b64 + ") found - check your config");
            }
        }

        PlayerProfile profile = Bukkit.createPlayerProfile(id, "Player");
        try {
            PlayerTextures profileTextures = profile.getTextures();
            profileTextures.setSkin(getUrlFromBase64(b64));
            profile.setTextures(profileTextures);
        } catch (MalformedURLException | NullPointerException e) {
            e.printStackTrace();
        }

        return profile;
    }

    private URL getUrlFromBase64(String base64) throws MalformedURLException {
        String decoded = new String(Base64.getDecoder().decode(base64));
        Matcher matcher = skinJsonPattern.matcher(decoded);
        if (matcher.find()) {
            return new URL(matcher.group(1));
        }
        return null;
    }

    private String getBase64FromUrl(URL url) {
        byte[] urlBytes = ("{textures:{skin:{url:" + url.toString() + "}}}").getBytes();
        return new String(Base64.getEncoder().encode(urlBytes));
    }
}
