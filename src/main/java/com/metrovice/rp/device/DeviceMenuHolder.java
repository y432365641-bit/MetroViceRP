package com.metrovice.rp.device;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
public final class DeviceMenuHolder implements InventoryHolder {
 private final String view; private Inventory inventory;
 public DeviceMenuHolder(String view){this.view=view;}
 public String view(){return view;}
 public void inventory(Inventory inventory){this.inventory=inventory;}
 @Override public Inventory getInventory(){return inventory;}
}