package me.wolfyscript.utilities.api.custom_items;

import me.wolfyscript.utilities.api.config.Config;
import me.wolfyscript.utilities.api.config.ConfigAPI;
import me.wolfyscript.utilities.api.config.JsonConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class CustomConfig extends Config {

    private String namespace;
    private String id;

    public CustomConfig(ConfigAPI configAPI, String namespace, String name, String path, String defaultPath, String defaultName, boolean override, String fileType) {
        super(configAPI, path, name, defaultPath, defaultName, fileType, override);
        this.namespace = namespace;
        this.id = namespace + ":" + name;
        if (getType().equals(Type.YAML)) {
            setSaveAfterValueSet(true);
        }
        setPathSeparator('.');
    }

    public CustomConfig(String jsonData, ConfigAPI configAPI, String namespace, String name, String defaultPath, String defaultName) {
        super(jsonData, configAPI, name, defaultPath, defaultName);
        this.namespace = namespace;
        this.id = namespace + ":" + name;
        setPathSeparator('.');
    }

    public CustomConfig(ConfigAPI configAPI, String namespace, String name, String defaultPath, String defaultName) {
        super(configAPI, name, defaultPath, defaultName);
        this.namespace = namespace;
        this.id = namespace + ":" + name;
        setPathSeparator('.');
    }

    public String getNamespace() {
        return namespace;
    }

    public String getId() {
        return id;
    }

    public void saveCustomItem(String path, CustomItem customItem) {
        if (customItem != null) {
            if (!customItem.getId().isEmpty() && !customItem.getId().equals("NULL")) {
                set(path + ".item_key", customItem.getId());
                set(path + ".custom_amount", customItem.getAmount() != CustomItems.getCustomItem(customItem.getId()).getAmount() ? customItem.getAmount() : 0);
            } else {
                setItem(path + ".item", new ItemStack(customItem));
            }
        }
    }

    public CustomItem getCustomItem(String path) {
        String id = getString(path + ".item_key");
        if (id != null && !id.isEmpty()) {
            CustomItem customItem = CustomItems.getCustomItem(id);
            if (get(path + ".custom_amount") != null) {
                int i = getInt(path + ".custom_amount");
                if (i != 0) {
                    customItem.setAmount(i);
                }
            }
            return customItem;
        }
        return new CustomItem(getItem(path + ".item"));
    }

    public void linkToFile(String namespace, String name, String path) {
        if(this.configuration instanceof JsonConfiguration){
            this.namespace = namespace;
            this.setName(name);
            this.id = namespace + ":" + name;
            this.configuration.setName(this.id);
            ((JsonConfiguration) this.configuration).linkToFile(path + "/" + name);
        }
    }
}
