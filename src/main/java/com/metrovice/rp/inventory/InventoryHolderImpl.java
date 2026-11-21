package com.metrovice.rp.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class InventoryHolderImpl implements InventoryHolder {
    private final int characterId;
    public InventoryHolderImpl(int characterId) { this.characterId = characterId; }
    public int characterId() { return characterId; }
    @Override public Inventory getInventory() { return null; }
}