package me.wolfyscript.utilities.api;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.wolfyscript.utilities.api.config.ConfigAPI;
import me.wolfyscript.utilities.api.inventory.InventoryAPI;
import me.wolfyscript.utilities.api.language.LanguageAPI;
import me.wolfyscript.utilities.api.utils.Legacy;
import me.wolfyscript.utilities.api.utils.Reflection;
import me.wolfyscript.utilities.main.Main;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WolfyUtilities {

    private Plugin plugin;

    private String CONSOLE_PREFIX;
    private String CHAT_PREFIX;

    private ConfigAPI configAPI;
    private InventoryAPI inventoryAPI;
    private LanguageAPI languageAPI;

    private static boolean hasLWC;
    private static boolean hasWorldGuard;
    private static boolean hasPlotSquared;

    public WolfyUtilities(Plugin plugin){
        this.plugin = plugin;
        Main.registerWolfyUtilities(this);
    }

    public LanguageAPI getLanguageAPI() {
        if(!hasLanguageAPI()){
            languageAPI = new LanguageAPI(this.plugin);
        }
        return languageAPI;
    }

    public boolean isLanguageEnabled(){
        return languageAPI != null;
    }

    public ConfigAPI getConfigAPI() {
        if(!hasConfigAPI()){
            configAPI = new ConfigAPI(this.plugin);
        }
        return configAPI;
    }

    public boolean isConfigEnabled(){
        return languageAPI != null;
    }

    public InventoryAPI getInventoryAPI() {
        if(!hasInventoryAPI()){
            inventoryAPI = new InventoryAPI(plugin, this);
        }
        return inventoryAPI;
    }

    public boolean hasInventoryAPI(){
        return inventoryAPI != null;
    }

    public boolean hasLanguageAPI(){
        return languageAPI != null;
    }

    public boolean hasConfigAPI(){
        return configAPI != null;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setCONSOLE_PREFIX(String CONSOLE_PREFIX) {
        this.CONSOLE_PREFIX = CONSOLE_PREFIX;
    }

    public void setCHAT_PREFIX(String CHAT_PREFIX) {
        this.CHAT_PREFIX = CHAT_PREFIX;
    }

    public String getCHAT_PREFIX() {
        return CHAT_PREFIX;
    }

    public String getCONSOLE_PREFIX() {
        return CONSOLE_PREFIX;
    }

    public boolean hasDebuggingMode() {
        return getConfigAPI().getConfig("main_config").getBoolean("debug");
    }

    public void sendConsoleMessage(String message) {
        message = CONSOLE_PREFIX + getLanguageAPI().getActiveLanguage().replaceKeys(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        Main.getInstance().getServer().getConsoleSender().sendMessage(message);
    }

    public void sendConsoleWarning(String message) {
        message = CONSOLE_PREFIX + "[WARN] " + getLanguageAPI().getActiveLanguage().replaceKeys(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        Main.getInstance().getServer().getConsoleSender().sendMessage(message);
    }

    public void sendConsoleMessage(String message, String... replacements) {
        message = CONSOLE_PREFIX + getLanguageAPI().getActiveLanguage().replaceKeys(message);
        List<String> keys = new ArrayList<>();
        Pattern pattern = Pattern.compile("%([A-Z]*?)(_*?)%");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            keys.add(matcher.group(0));
        }
        for (int i = 0; i < keys.size(); i++) {
            message = message.replace(keys.get(i), replacements[i]);
        }
        message = ChatColor.translateAlternateColorCodes('&', message);
        plugin.getServer().getConsoleSender().sendMessage(message);
    }

    public void sendPlayerMessage(Player player, String message) {
        message = CHAT_PREFIX + getLanguageAPI().getActiveLanguage().replaceKeys(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        player.sendMessage(message);
    }

    public void sendDebugMessage(String message) {
        if (hasDebuggingMode()) {
            String prefix = ChatColor.translateAlternateColorCodes('&', "[&4CC&r] ");
            message = ChatColor.translateAlternateColorCodes('&', message);
            //message = ChatColor.stripColor(message);
            List<String> messages = new ArrayList<>();
            if (message.length() > 70) {
                int count = message.length() / 70;
                for (int text = 0; text <= count; text++) {
                    if (text < count) {
                        messages.add(message.substring(text * 70, 70 + 70 * text));
                    } else {
                        messages.add(message.substring(text * 70));
                    }
                }
                for (String result : messages) {
                    Main.getInstance().getServer().getConsoleSender().sendMessage(prefix + result);
                }
            } else {
                message = prefix + message;
                Main.getInstance().getServer().getConsoleSender().sendMessage(message);
            }

        }
    }

    static Random random = new Random();

    public static boolean hasAquaticUpdate() {
        String pkgname = Main.getInstance().getServer().getClass().getPackage().getName();
        String combatVersion = "v1_13_R0".replace("_", "").replace("R0", "").replace("R1", "").replace("R2", "").replace("R3", "").replace("R4", "").replace("R5", "").replaceAll("[a-z]", "");
        String version = pkgname.substring(pkgname.lastIndexOf('.') + 1).replace("_", "").replace("R0", "").replace("R1", "").replace("R2", "").replace("R3", "").replace("R4", "").replace("R5", "").replaceAll("[a-z]", "");
        return Integer.parseInt(version) >= Integer.parseInt(combatVersion);
    }

    public static boolean hasCombatUpdate() {
        String pkgname = Main.getInstance().getServer().getClass().getPackage().getName();
        String combatVersion = "v1_9_R0".replace("_", "").replace("R0", "").replace("R1", "").replace("R2", "").replace("R3", "").replace("R4", "").replace("R5", "").replaceAll("[a-z]", "");
        String version = pkgname.substring(pkgname.lastIndexOf('.') + 1).replace("_", "").replace("R0", "").replace("R1", "").replace("R2", "").replace("R3", "").replace("R4", "").replace("R5", "").replaceAll("[a-z]", "");
        return Integer.parseInt(version) >= Integer.parseInt(combatVersion);
    }

    public static boolean hasSpecificUpdate(String versionString) {
        String pkgname = Main.getInstance().getServer().getClass().getPackage().getName();
        String localeVersion = "v" + versionString + "_R0";
        localeVersion = localeVersion.replace("_", "").replace("R0", "").replace("R1", "").replace("R2", "").replace("R3", "").replace("R4", "").replace("R5", "").replaceAll("[a-z]", "");
        String version = pkgname.substring(pkgname.lastIndexOf('.') + 1).replace("_", "").replace("R0", "").replace("R1", "").replace("R2", "").replace("R3", "").replace("R4", "").replace("R5", "").replaceAll("[a-z]", "");
        return Integer.parseInt(version) >= Integer.parseInt(localeVersion);
    }

    public static boolean hasSpigot() {
        return hasClass("org.spigotmc.Metrics");
    }

    public static void setLWC() {
        hasLWC = hasClass("com.griefcraft.lwc.LWC");
    }

    public static void setPlotSquared() {
        hasPlotSquared = hasClass("com.intellectualcrafters.plot.api.PlotAPI");
    }

    public static void setWorldGuard() {
        hasWorldGuard = hasClass("com.sk89q.worldguard.WorldGuard");
    }

    public static boolean hasWorldGuard(){
        return hasWorldGuard;
    }

    public static boolean hasPlotSquared(){
        return hasPlotSquared;
    }

    public static boolean hasLWC(){
        return hasLWC;
    }

    public static boolean hasClass(String path){
        try {
            Class.forName(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasPermission(CommandSender sender, String permCode) {
        List<String> permissions = Arrays.asList(permCode.split("\\."));
        StringBuilder permission = new StringBuilder();
        if (sender.hasPermission("*")) {
            return true;
        }
        for (String perm : permissions) {
            permission.append(perm);
            if (permissions.indexOf(perm) < permissions.size() - 1) {
                permission.append(".*");
            }
            if (sender.hasPermission(permission.toString())) {
                return true;
            }
            permission.replace(permission.length() - 2, permission.length(), "");
            permission.append(".");
        }
        return false;
    }

    //Not tested yet!!

    public static void sendParticles(Player player, String particle, boolean biggerRadius, float x, float y, float z, float xOffset, float yOffset, float zOffset, int count, float particledata, int... data) {
        try {
            Object enumParticles = Reflection.getNMS("EnumParticle").getField(particle).get(null);
            Constructor<?> particleConstructor = Reflection.getNMS("PacketPlayOutWorldParticles").getConstructor(
                    Reflection.getNMS("EnumParticle"), boolean.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, int.class, int[].class);
            Object packet = particleConstructor.newInstance(enumParticles, biggerRadius, x, y, z, xOffset, yOffset, zOffset, particledata, count, data);
            sendPacket(player, packet);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", Reflection.getNMS("Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCode() {
        Random random = new Random();
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final int x = alphabet.length();
        StringBuilder sB = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sB.append(alphabet.charAt(random.nextInt(x)));
        }
        return sB.toString();
    }

    public static ItemStack getCustomHead(String value) {
        if (value.startsWith("http://textures")) {
            value = new String(Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", value).getBytes()));
        }
        return getSkullByValue(value);
    }

    public static ItemStack getSkullByValue(String value) {
        ItemStack itemStack;
        if (WolfyUtilities.hasAquaticUpdate()) {
            itemStack = new ItemStack(Material.PLAYER_HEAD);
        } else {
            itemStack = new ItemStack(Material.PLAYER_HEAD, 1, (short) 0, (byte) 3);
        }
        if (value != null && !value.isEmpty()) {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", value));
            Field profileField = null;
            try {
                profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
            }
            try {
                profileField.set(skullMeta, profile);
                itemStack.setItemMeta(skullMeta);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return itemStack;
    }

    public static ItemMeta getSkullmeta(String value, SkullMeta skullMeta) {
        if (value != null && !value.isEmpty()) {
            String texture = value;
            if (value.startsWith("https://") || value.startsWith("http://")) {
                byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", value).getBytes());
                texture = new String(encodedData);
            }
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            Field profileField = null;
            try {
                profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
            } catch (NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
            }
            try {
                profileField.set(skullMeta, profile);
                return skullMeta;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return skullMeta;
    }

    /*
    Sets a § before every letter!
    example:
        Hello World -> §H§e§l§l§o§ §W§o§r§l§d

    Because of this the String will be invisible in Minecraft!
     */
    public static String hideString(String hide) {
        char[] data = new char[hide.length() * 2];
        for (int i = 0; i < data.length; i += 2) {
            data[i] = 167;
            data[i + 1] = hide.charAt(i == 0 ? 0 : i / 2);
        }
        return new String(data);
    }

    public static String unhideString(String unhide) {
        return unhide.replace("§", "");
    }

    public static Enchantment getEnchantment(String enchantNmn) {
        try {
            if (!WolfyUtilities.hasAquaticUpdate()) {
                return Legacy.getEnchantment(enchantNmn);
            } else {
                return Enchantment.getByKey(NamespacedKey.minecraft(enchantNmn.toLowerCase()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Sound getSound(String sound) {
        return Sound.valueOf(sound);
    }

    public static Sound getSound(String legacy, String notLegacy) {
        if (WolfyUtilities.hasAquaticUpdate()) {
            return Sound.valueOf(notLegacy);
        } else {
            return Sound.valueOf(legacy);
        }
    }

    public static boolean isSkull(ItemStack itemStack) {
        if (itemStack.getType() == Material.PLAYER_HEAD) {
            if (WolfyUtilities.hasAquaticUpdate()) {
                return true;
            } else return itemStack.getData().getData() == (byte) 3;
        }
        return false;
    }
}