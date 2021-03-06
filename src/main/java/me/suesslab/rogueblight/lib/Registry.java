package me.suesslab.rogueblight.lib;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.suesslab.rogueblight.SubsystemManager;
import me.suesslab.rogueblight.item.ItemContainer;
import me.suesslab.rogueblight.basegame.Stone;
import me.suesslab.rogueblight.entity.Entity;
import me.suesslab.rogueblight.entity.EntityType;
import me.suesslab.rogueblight.item.Inventory;
import me.suesslab.rogueblight.item.ItemType;
import me.suesslab.rogueblight.world.IWorld;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public final class Registry implements Subsystem {

    private ArrayList<EntityType> entityTypes;
    private ArrayList<ItemType> itemTypes;
    private SubsystemManager manager;
    private JsonObject allRegistryConfigs;

    public Registry() {
        entityTypes = new ArrayList<>();
        itemTypes = new ArrayList<>();
    }

    public void registerEntityType(EntityType type) {
        entityTypes.add(type);
    }

    public void registerItemType(ItemType type) {
        itemTypes.add(type);
    }

    @Override
    public void init(SubsystemManager manager) {
        this.manager = manager;
        try {
            JsonParser parser = new JsonParser();
            System.out.println(manager.getDataPath() + "/" + manager.getTypeConfigFileName());
            FileReader reader = new FileReader(manager.getDataPath() + "/" + manager.getTypeConfigFileName());
            allRegistryConfigs = parser.parse(reader).getAsJsonObject();
        } catch (FileNotFoundException e) {
            manager.getLogger().severe("Could not open types.json at path " + manager.getDataPath());
        }
        //TODO: Find better way to register entities.
        registerEntityType(new ItemContainer());
        registerItemType(new Stone());
    }

    @Override
    public void stop() {

    }

    public boolean checkIfEntityTypeExists(String typeName) {
        for (EntityType type : entityTypes) {
            if (type.getName().equals(typeName)) {
                return true;
            }
        }
        return false;
    }



    public void registerType(EntityType type) {
        manager.getLogger().info("Registering new entity type " + type.getName());
        Optional<JsonObject> config = attemptToFindConfig(type.getName());
        if (config.isPresent()) {
            type.setConfig(config.get());
        } else {
            manager.getLogger().warning("Unable to find existing configuration for " + type.getName() + ", using empty configuration");
            type.setConfig(new JsonObject());
        }
        entityTypes.add(type);
    }

    public Optional<Entity> createEntityInWorld(JsonObject config, IWorld world) {
        if (!config.has("type")) {
            manager.getLogger().warning("Malformed JsonObject, type not found, cannot instantiate entity");
            return Optional.empty();
        }
        Optional<EntityType> type = attemptToFindEntityType(config.get("type").getAsString());
        if (type.isPresent()) {
            return Optional.of(type.get().create(config, world));
        }
        manager.getLogger().warning("Unable to find matching type " + config.get("type").getAsString());
        return Optional.empty();
    }

    public boolean createItemInInventory(Inventory inv, JsonObject config) {
        if (!config.has("type")) {
            manager.getLogger().warning("Malformed JsonObject, type not found, cannot instantiate item");
            return false;
        }
        Optional<ItemType> type = attemptToFindItemType(config.get("type").getAsString());

        if (type.isPresent()) {
            type.get().create(config, inv);
        }
        manager.getLogger().warning("");
        manager.getLogger().warning("Unable to find matching type " + config.get("type").getAsString());
        return false;
    }

    public void addPlugin(IPlugin plugin) {
        plugin.getEntityTypes().forEach(this::registerEntityType);
        plugin.getItemTypes().forEach(this::registerItemType);
    }

    public void resetRegistry() {
        itemTypes.clear();
        entityTypes.clear();
    }

    private Optional<EntityType> attemptToFindEntityType(String typeName) {
        for (EntityType type : entityTypes) {
            if (type.getName().equals(typeName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private Optional<ItemType> attemptToFindItemType(String typeName) {
        for (ItemType type : itemTypes) {
            if (type.getName().equals(typeName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private Optional<JsonObject> attemptToFindConfig(String typeName) {
        Set<Map.Entry<String, JsonElement>> entrySet = allRegistryConfigs.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            if (entry.getKey().equals(typeName)) {
                return Optional.of(entry.getValue().getAsJsonObject());
            }
        }
        return Optional.empty();
    }
}
