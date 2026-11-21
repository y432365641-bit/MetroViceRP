package com.metrovice.rp.character;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class CharacterMenuHolder implements InventoryHolder {
    private final UUID owner;
    private Inventory inventory;

    public CharacterMenuHolder(UUID owner) { this.owner = owner; }
    public UUID owner() { return owner; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}