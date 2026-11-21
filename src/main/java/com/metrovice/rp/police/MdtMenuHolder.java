package com.metrovice.rp.police;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MdtMenuHolder implements InventoryHolder {
    private final String view;
    public MdtMenuHolder(String view) { this.view = view; }
    public String view() { return view; }
    @Override public Inventory getInventory() { return null; }
}
