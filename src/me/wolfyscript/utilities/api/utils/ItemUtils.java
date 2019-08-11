package me.wolfyscript.utilities.api.utils;

import com.google.common.io.BaseEncoding;
import com.sun.istack.internal.NotNull;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.main.Main;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nullable;
import javax.management.ReflectionException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ItemUtils {

    /**
     * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string
     * for sending with {@link net.md_5.bungee.api.chat.BaseComponent}'s.
     *
     * @param itemStack the item to convert
     * @return the Json string representation of the item
     */
    public static String convertItemStackToJson(ItemStack itemStack) {
        // ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
        Class<?> craftItemStackClazz = Reflection.getOBC("inventory.CraftItemStack");
        Method asNMSCopyMethod = Reflection.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

        // NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
        Class<?> nmsItemStackClazz = Reflection.getNMS("ItemStack");
        Class<?> nbtTagCompoundClazz = Reflection.getNMS("NBTTagCompound");
        Method saveNmsItemStackMethod = Reflection.getMethod(nmsItemStackClazz, "save", nbtTagCompoundClazz);

        Object nmsNbtTagCompoundObj; // This will just be an empty NBTTagCompound instance to invoke the saveNms method
        Object nmsItemStackObj; // This is the net.minecraft.server.ItemStack object received from the asNMSCopy method
        Object itemAsJsonObject; // This is the net.minecraft.server.ItemStack after being put through saveNmsItem method

        try {
            nmsNbtTagCompoundObj = nbtTagCompoundClazz.newInstance();
            nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
            itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
        } catch (Throwable t) {
            Main.getMainUtil().sendConsoleMessage("failed to serialize itemstack to nms item");
            Main.getMainUtil().sendConsoleMessage(t.toString());
            for (StackTraceElement element : t.getStackTrace()) {
                Main.getMainUtil().sendConsoleMessage(element.toString());
            }
            return null;
        }
        // Return a string representation of the serialized object
        return itemAsJsonObject.toString();
    }

    /*
    Prepare and configure the ItemStack for the GUI!
     */
    public static ItemStack[] createItem(ItemStack itemStack, String displayName, String[] helpLore, String... normalLore){
        ItemStack[] itemStacks = new ItemStack[2];
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName + WolfyUtilities.hideString(Main.getMainConfig().getString("securityCode"))));
        List<String> lore = new ArrayList<>();
        if (normalLore != null && normalLore.length > 0) {
            for (String row : normalLore) {
                if (!row.isEmpty()) {
                    lore.add(row.equalsIgnoreCase("<empty>") ? "" : org.bukkit.ChatColor.translateAlternateColorCodes('&', row));
                }
            }
        }
        if (lore.size() > 0) {
            itemMeta.setLore(lore);
        }
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        itemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        itemStack.setItemMeta(itemMeta);
        itemStacks[0] = itemStack;
        ItemStack helpItem = new ItemStack(itemStack);
        ItemMeta helpMeta = helpItem.getItemMeta();
        if (helpLore != null && helpLore.length > 0) {
            for (String row : helpLore) {
                if (!row.isEmpty()) {
                    lore.add(row.equalsIgnoreCase("<empty>") ? "" : ChatColor.translateAlternateColorCodes('&', row));
                }
            }
        }
        helpMeta.setLore(lore);
        helpItem.setItemMeta(helpMeta);
        itemStacks[1] = helpItem;
        return itemStacks;
    }

    /*
    This method may be problematic if using NBT data.
    The data maybe can't be saved and loaded correctly!
     */
    @Deprecated
    public static String serializeItemStack(ItemStack is) {
        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(outputStream);
            bukkitOutputStream.writeObject(is);
            bukkitOutputStream.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize ItemStack!", e);
        }
    }

    public static ItemStack deserializeItemStack(String data){
        return deserializeItemStack(Base64.getDecoder().decode(data));
    }

    public static ItemStack deserializeItemStack(byte[] bytes) {
        try {
            try{
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(inputStream);
                Object itemStack = bukkitInputStream.readObject();
                if(itemStack instanceof ItemStack){
                    return (ItemStack) itemStack;
                }
            }catch (StreamCorruptedException ex){
                return deserializeNMSItemStack(bytes);
            }
            return null;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String serializeNMSItemStack(ItemStack itemStack){
        if(itemStack == null) return "null";
        ByteArrayOutputStream outputStream = null;
        try{
            Class<?> nbtTagCompoundClass = Reflection.getNMS("NBTTagCompound");
            Constructor<?> nbtTagCompoundConstructor = nbtTagCompoundClass.getConstructor();
            Object nbtTagCompound = nbtTagCompoundConstructor.newInstance();
            Object nmsItemStack = Reflection.getOBC("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class).invoke(null, itemStack);
            Reflection.getNMS("ItemStack").getMethod("save", nbtTagCompoundClass).invoke(nmsItemStack, nbtTagCompound);
            outputStream = new ByteArrayOutputStream();
            Reflection.getNMS("NBTCompressedStreamTools").getMethod("a", nbtTagCompoundClass, OutputStream.class).invoke(null, nbtTagCompound, outputStream);
        }catch(SecurityException | NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e){
            e.printStackTrace();
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static ItemStack deserializeNMSItemStack(String data){
        return deserializeNMSItemStack(Base64.getDecoder().decode(data));
    }

    public static ItemStack deserializeNMSItemStack(byte[] bytes){
        if(bytes == null) return null;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        Class<?> nbtTagCompoundClass = Reflection.getNMS("NBTTagCompound");
        Class<?> nmsItemStackClass = Reflection.getNMS("ItemStack");
        Object nbtTagCompound = null;
        ItemStack itemStack = null;
        try{
            nbtTagCompound = Reflection.getNMS("NBTCompressedStreamTools").getMethod("a", InputStream.class).invoke(null, inputStream);
            Object craftItemStack = nmsItemStackClass.getMethod("a", nbtTagCompoundClass).invoke(nmsItemStackClass, nbtTagCompound);
            itemStack = (ItemStack)Reflection.getOBC("inventory.CraftItemStack").getMethod("asBukkitCopy", nmsItemStackClass).invoke(null, craftItemStack);
        }catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e){
            e.printStackTrace();
        }

        return itemStack;
    }

    /*
    Sets value to the lore. It will be hidden.
     */
    public static ItemMeta setToCCSettings(ItemMeta itemMeta, String key, Object value){
        JSONObject obj = getCCSettings(itemMeta);
        List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
        if(obj == null){
            obj = new JSONObject(new HashMap<String, Object>());
            obj.put(key, value);
            lore.add(WolfyUtilities.hideString("itemSettings"+obj.toString()));
        }else{
            obj.put(key, value);
            for(int i = 0; i < lore.size(); i++){
                String line = WolfyUtilities.unhideString(lore.get(i));
                if(line.startsWith("itemSettings")){
                    lore.set(i, WolfyUtilities.hideString("itemSettings"+obj.toString()));
                }
            }
        }
        itemMeta.setLore(lore);
        return itemMeta;
    }

    public static ItemStack setToCCSettings(ItemStack itemStack, String key, Object value){
        itemStack.setItemMeta(setToCCSettings(itemStack.getItemMeta(), key, value));
        return itemStack;
    }

    @Nullable
    public static Object getFromCCSettings(ItemMeta itemMeta, String key){
        if(hasCCSettings(itemMeta)){
            return getCCSettings(itemMeta).get(key);
        }
        return null;
    }

    public static Object getFromCCSettings(ItemStack itemStack, String key){
        return getFromCCSettings(itemStack.getItemMeta(), key);
    }

    public static boolean isInCCSettings(ItemStack itemStack, String key){
        return getFromCCSettings(itemStack, key) != null;
    }

    public static boolean isInCCSettings(ItemMeta itemMeta, String key){
        return getFromCCSettings(itemMeta, key) != null;
    }

    public static boolean hasCCSettings(@NotNull ItemStack itemStack){
        return getCCSettings(itemStack.getItemMeta()) != null;
    }

    public static boolean hasCCSettings(@Nullable ItemMeta itemMeta){
        return getCCSettings(itemMeta) != null;
    }

    public static JSONObject getCCSettings(@NotNull ItemStack itemStack){
        return getCCSettings(itemStack.getItemMeta());
    }

    @Nullable
    public static JSONObject getCCSettings(@Nullable ItemMeta itemMeta){
        if (itemMeta != null && itemMeta.hasLore()) {
            List<String> lore = itemMeta.getLore();
            for (String line : lore) {
                String cleared = WolfyUtilities.unhideString(line);
                if (cleared.startsWith("itemSettings")) {
                    try {
                        JSONObject obj = (JSONObject) new JSONParser().parse(cleared.replace("itemSettings", ""));
                        return obj;
                    } catch (ParseException e) {
                        Main.getMainUtil().sendConsoleWarning("Error getting JSONObject from String:");
                        Main.getMainUtil().sendConsoleWarning("" + cleared);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /*
        Custom Item Damage!
     */

    //itemSettings{"damage":<damage>,"durability":<total_durability>,"durability_tag":""}

    public static boolean hasCustomDurability(@NotNull ItemStack itemStack) {
        return hasCustomDurability(itemStack.getItemMeta());
    }

    public static boolean hasCustomDurability(@Nullable ItemMeta itemMeta) {
        JSONObject obj = getCCSettings(itemMeta);
        if (obj != null) {
            return ((Set<String>) obj.keySet()).contains("durability");
        }
        return false;
    }

    /*
    Sets the custom durability to the ItemStack and adds damage of 0 if it not exists.
    Returns the ItemStack with the new lore.
     */
    public static void setCustomDurability(ItemStack itemStack, int durability) {
        setCustomDurability(itemStack.getItemMeta(), durability);
    }

    public static void setCustomDurability(ItemMeta itemMeta, int durability) {
        setToCCSettings(itemMeta, "durability", durability);
        setDurabilityTag(itemMeta);
    }

    public static int getCustomDurability(ItemStack itemStack){
        return getCustomDurability(itemStack.getItemMeta());
    }

    public static int getCustomDurability(ItemMeta itemMeta){
        if(getFromCCSettings(itemMeta, "durability") != null){
            return NumberConversions.toInt(getFromCCSettings(itemMeta, "durability"));
        }
        return 0;
    }

    public static void setDamage(ItemStack itemStack, int damage){
        ItemMeta itemMeta = itemStack.getItemMeta();
        if(itemMeta instanceof Damageable){
            ((Damageable) itemMeta).setDamage((int) (itemStack.getType().getMaxDurability() * ((double) damage / (double) getCustomDurability(itemStack))));
        }
        setDamage(itemMeta, damage);
        itemStack.setItemMeta(itemMeta);
    }

    public static void setDamage(ItemMeta itemMeta, int damage){
        setToCCSettings(itemMeta, "damage", damage);
        setDurabilityTag(itemMeta);
    }

    public static int getDamage(ItemStack itemStack){
        return getDamage(itemStack.getItemMeta());
    }

    public static int getDamage(ItemMeta itemMeta){
        if(getFromCCSettings(itemMeta, "damage") != null){
            int damage = NumberConversions.toInt(getFromCCSettings(itemMeta, "damage"));
            return damage;
        }
        return 0;
    }

    public static void setDurabilityTag(ItemStack itemStack){
        ItemMeta itemMeta = itemStack.getItemMeta();
        setDurabilityTag(itemMeta);
        itemStack.setItemMeta(itemMeta);
    }

    public static void setDurabilityTag(ItemMeta itemMeta){
        if(!getDurabilityTag(itemMeta).isEmpty() && !getDurabilityTag(itemMeta).equals("")){
            List<String> lore = itemMeta.getLore() != null ? itemMeta.getLore() : new ArrayList<>();
            for(int i = 0; i < lore.size(); i++){
                String line = WolfyUtilities.unhideString(lore.get(i));
                if(line.startsWith("durability_tag")){
                    lore.remove(i);
                }
            }
            lore.add(lore.size() > 0 ? lore.size()-1 : 0, WolfyUtilities.hideString("durability_tag") + WolfyUtilities.translateColorCodes(getDurabilityTag(itemMeta).replace("%DUR%", String.valueOf(getCustomDurability(itemMeta)-getDamage(itemMeta))).replace("%MAX_DUR%", String.valueOf(getCustomDurability(itemMeta)))));
            itemMeta.setLore(lore);
        }
    }

    public static void setDurabilityTag(ItemMeta itemMeta, String value){
        setToCCSettings(itemMeta, "durability_tag", value);
        setDurabilityTag(itemMeta);
    }

    public static void setDurabilityTag(ItemStack itemStack, String value){
        ItemMeta itemMeta = itemStack.getItemMeta();
        setDurabilityTag(itemMeta, value);
        itemStack.setItemMeta(itemMeta);
    }

    public static String getDurabilityTag(ItemStack itemStack){
        return getDurabilityTag(itemStack.getItemMeta());
    }

    public static String getDurabilityTag(ItemMeta itemMeta){
        if(getFromCCSettings(itemMeta, "durability_tag") != null){
            return (String) getFromCCSettings(itemMeta, "durability_tag");
        }
        return "";
    }
}