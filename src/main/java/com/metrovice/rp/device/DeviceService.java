package com.metrovice.rp.device;
import com.metrovice.rp.MetroViceRP;
import com.metrovice.rp.character.CharacterProfile;
import com.metrovice.rp.character.CharacterService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public final class DeviceService implements Listener {
 private final JavaPlugin plugin; private final CharacterService characters;
 private final NamespacedKey phoneKey,radioKey;
 private final Map<UUID,Integer> channels=new ConcurrentHashMap<>();
 private final Map<UUID,Boolean> pendingMessage=new ConcurrentHashMap<>();
 public DeviceService(JavaPlugin plugin,CharacterService characters){
  this.plugin=plugin;this.characters=characters;
  phoneKey=new NamespacedKey(plugin,"mvrp_phone");radioKey=new NamespacedKey(plugin,"mvrp_radio");
 }
 public ItemStack createPhone(){return marked(Material.CLOCK,phoneKey,"&b&lMetroPhone","&7هاتف Metro Vice RP","&eاضغط بزر الفأرة الأيمن لفتحه");}
 public ItemStack createRadio(){return marked(Material.GOAT_HORN,radioKey,"&9&lMetro Radio","&7جهاز اتصال لاسلكي","&eاضغط بزر الفأرة الأيمن لاختيار القناة");}
 private ItemStack marked(Material material,NamespacedKey key,String name,String... lore){
  ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();
  meta.setDisplayName(MetroViceRP.color(name));meta.setLore(java.util.Arrays.stream(lore).map(MetroViceRP::color).toList());
  meta.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;
 }
 @EventHandler public void onInteract(PlayerInteractEvent event){
  Action action=event.getAction();if(action!=Action.RIGHT_CLICK_AIR&&action!=Action.RIGHT_CLICK_BLOCK)return;
  ItemStack held=event.getItem();if(held==null||!held.hasItemMeta())return;
  var pdc=held.getItemMeta().getPersistentDataContainer();Player player=event.getPlayer();
  if(pdc.has(phoneKey,PersistentDataType.BYTE)){event.setCancelled(true);openPhone(player);}
  else if(pdc.has(radioKey,PersistentDataType.BYTE)){event.setCancelled(true);openRadio(player);}
 }
 private void openPhone(Player player){
  DeviceMenuHolder holder=new DeviceMenuHolder("phone");Inventory inv=Bukkit.createInventory(holder,27,"§1§lMETRO VICE §8| §bPHONE");holder.inventory(inv);
  inv.setItem(10,button(Material.WRITABLE_BOOK,"&bالرسائل","&7إرسال رسالة للاعب متصل","&eاكتب: الاسم: الرسالة"));
  inv.setItem(13,button(Material.PLAYER_HEAD,"&dبطاقة الشخصية","&7عرض بيانات الشخصية النشطة"));
  inv.setItem(16,button(Material.GOLD_INGOT,"&6البنك","&7عرض رصيد حسابك"));
  inv.setItem(22,button(Material.BARRIER,"&cإغلاق"));player.openInventory(inv);
 }
 private void openRadio(Player player){
  DeviceMenuHolder holder=new DeviceMenuHolder("radio");Inventory inv=Bukkit.createInventory(holder,27,"§1§lMETRO VICE §8| §9RADIO");holder.inventory(inv);
  for(int c=1;c<=5;c++)inv.setItem(9+c,button(Material.NOTE_BLOCK,"&bقناة "+c,"&7اضغط لاختيار القناة","&8الحالية: "+(channels.getOrDefault(player.getUniqueId(),1)==c?"§aنعم":"§7لا")));
  inv.setItem(22,button(Material.BARRIER,"&cإغلاق"));player.openInventory(inv);
 }
 @EventHandler public void onClick(InventoryClickEvent event){
  if(!(event.getView().getTopInventory().getHolder() instanceof DeviceMenuHolder holder))return;
  event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player)||event.getClickedInventory()!=event.getView().getTopInventory())return;
  int slot=event.getRawSlot();if(slot==22){player.closeInventory();return;}
  if(holder.view().equals("phone")){
   if(slot==10){pendingMessage.put(player.getUniqueId(),true);player.closeInventory();player.sendMessage(MetroViceRP.color("&bاكتب: &fاسم اللاعب: نص الرسالة"));}
   else if(slot==13){player.closeInventory();showIdentity(player);}
   else if(slot==16){player.closeInventory();player.performCommand("bank balance");}
  }else if(holder.view().equals("radio")&&slot>=10&&slot<=14){
   int channel=slot-9;channels.put(player.getUniqueId(),channel);player.closeInventory();
   player.sendMessage(MetroViceRP.color("&9تم ضبط الراديو على القناة &b"+channel+"&9. للإرسال: &f/radio رسالتك"));
  }
 }
 private void showIdentity(Player player){
  try{CharacterProfile p=characters.active(player.getUniqueId()).orElse(null);if(p==null){player.sendMessage(MetroViceRP.color("&cلا توجد شخصية نشطة."));return;}
   player.sendMessage(MetroViceRP.color("&8&m--------- &bبطاقة الشخصية &8&m---------"));
   player.sendMessage(MetroViceRP.color("&fالاسم: &b"+p.name()));player.sendMessage(MetroViceRP.color("&fالعمر: &b"+p.age()));
   player.sendMessage(MetroViceRP.color("&fرقم الهوية: &b"+p.id()));player.sendMessage(MetroViceRP.color("&fالوظيفة: &b"+p.job()));
   player.sendMessage(MetroViceRP.color("&fالرتبة: &b"+p.rank()));
  }catch(SQLException ex){plugin.getLogger().severe("Phone identity lookup failed: "+ex.getMessage());player.sendMessage(MetroViceRP.color("&cتعذر تحميل بيانات الشخصية."));}
 }
 @EventHandler public void onChat(AsyncPlayerChatEvent event){
  if(!pendingMessage.remove(event.getPlayer().getUniqueId(),true))return;event.setCancelled(true);
  String input=event.getMessage().strip();int split=input.indexOf(':');Player sender=event.getPlayer();
  if(split<1||split==input.length()-1){sender.sendMessage(MetroViceRP.color("&cالصيغة: الاسم: الرسالة"));return;}
  Player target=Bukkit.getPlayerExact(input.substring(0,split).strip());String message=input.substring(split+1).strip();
  if(target==null||message.isEmpty()){sender.sendMessage(MetroViceRP.color("&cاللاعب غير متصل أو الرسالة فارغة."));return;}
  Bukkit.getScheduler().runTask(plugin,()->{sender.sendMessage(MetroViceRP.color("&d[هاتفك -> "+target.getName()+"] &f"+message));target.sendMessage(MetroViceRP.color("&d[رسالة هاتف من "+sender.getName()+"] &f"+message));});
 }
 public boolean sendRadioMessage(Player sender,String message){
  if(!hasRadio(sender)){sender.sendMessage(MetroViceRP.color("&cيجب أن تحمل الراديو لإرسال الرسائل."));return true;}
  if(message==null||message.isBlank()){sender.sendMessage(MetroViceRP.color("&eالاستخدام: /radio <رسالتك>"));return true;}
  int channel=channels.getOrDefault(sender.getUniqueId(),1);String line=MetroViceRP.color("&9[لاسلكي "+channel+"] &b"+sender.getName()+"&f: "+message);
  for(Player target:Bukkit.getOnlinePlayers())if(hasRadio(target)&&channels.getOrDefault(target.getUniqueId(),1)==channel)target.sendMessage(line);
  return true;
 }
 private boolean hasRadio(Player player){
  if(isRadio(player.getInventory().getItemInMainHand())||isRadio(player.getInventory().getItemInOffHand()))return true;
  for(ItemStack item:player.getInventory().getContents())if(isRadio(item))return true;return false;
 }
 private boolean isRadio(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(radioKey,PersistentDataType.BYTE);}
 private ItemStack button(Material material,String name,String... lore){
  ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setDisplayName(MetroViceRP.color(name));
  meta.setLore(java.util.Arrays.stream(lore).map(MetroViceRP::color).toList());item.setItemMeta(meta);return item;
 }
}