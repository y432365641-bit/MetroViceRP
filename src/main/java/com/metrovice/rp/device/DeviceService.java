package com.metrovice.rp.device;

import com.metrovice.rp.MetroViceRP;
import com.metrovice.rp.character.CharacterProfile;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.department.DepartmentService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeviceService implements Listener {
    private enum Input { MESSAGE, POST, SET_PIN, UNLOCK, SET_CHANNEL }
    private final JavaPlugin plugin;
    private final CharacterService characters;
    private final DepartmentService departments;
    private final NamespacedKey phoneKey, radioKey, photoKey;
    private final Map<UUID,Double> channels = new ConcurrentHashMap<>();
    private final Map<UUID,Input> waiting = new ConcurrentHashMap<>();
    private final Map<UUID,Boolean> notifications = new ConcurrentHashMap<>();
    private final Map<UUID,String> pins = new ConcurrentHashMap<>();
    private final Deque<String> feed = new ArrayDeque<>();

    public DeviceService(JavaPlugin plugin, CharacterService characters, DepartmentService departments) {
        this.plugin=plugin; this.characters=characters; this.departments=departments;
        phoneKey=new NamespacedKey(plugin,"mvrp_phone");
        radioKey=new NamespacedKey(plugin,"mvrp_radio");
        photoKey=new NamespacedKey(plugin,"mvrp_phone_photo");
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
        var data=held.getItemMeta().getPersistentDataContainer();Player player=event.getPlayer();
        if(data.has(phoneKey,PersistentDataType.BYTE)){
            event.setCancelled(true);
            if(pins.containsKey(player.getUniqueId()))ask(player,Input.UNLOCK,"&bالهاتف مقفل. اكتب رمز القفل:");
            else openPhone(player);
        }else if(data.has(radioKey,PersistentDataType.BYTE)){event.setCancelled(true);openRadio(player);}
    }
    private void openPhone(Player p){
        DeviceMenuHolder h=new DeviceMenuHolder("phone");Inventory inv=Bukkit.createInventory(h,27,"§1§lMETRO VICE §8| §bPHONE");h.inventory(inv);
        inv.setItem(10,button(Material.WRITABLE_BOOK,"&bالرسائل","&7إرسال رسالة خاصة للاعب متصل"));
        inv.setItem(11,button(Material.PLAYER_HEAD,"&dبطاقة الهوية","&7بيانات شخصيتك الحالية"));
        inv.setItem(12,button(Material.GOLD_INGOT,"&6البنك","&7النقد ورصيد البنك"));
        inv.setItem(13,button(Material.SPYGLASS,"&eكاميرا الموقع","&7حفظ لقطة لإحداثياتك داخل المدينة"));
        inv.setItem(14,button(Material.MAP,"&aلقطات الموقع","&7استعرض اللقطات المحفوظة"));
        inv.setItem(15,button(Material.PAPER,"&dM - التواصل الاجتماعي","&7آخر المنشورات وانشر تحديثًا"));
        inv.setItem(16,button(Material.COMPARATOR,"&7الإعدادات","&7الإشعارات ورمز قفل الهاتف"));
        inv.setItem(22,button(Material.BARRIER,"&cإغلاق"));p.openInventory(inv);
    }
    private void openRadio(Player p){
        DeviceMenuHolder h=new DeviceMenuHolder("radio");
        Inventory inv=Bukkit.createInventory(h,27,"§1§lMETRO VICE §8| §9الراديو");
        h.inventory(inv);
        double current=channels.getOrDefault(p.getUniqueId(),1.0);
        inv.setItem(13,button(Material.NOTE_BLOCK,"&bتحديد التردد",
                "&7التردد الحالي: &f"+formatFrequency(current),
                "&7النطاق: 1.00 إلى 100.00",
                "&eاضغط لإدخال تردد"));
        inv.setItem(22,button(Material.BARRIER,"&cإغلاق"));
        p.openInventory(inv);
    }
    private void openGallery(Player p){
        DeviceMenuHolder h=new DeviceMenuHolder("gallery");Inventory inv=Bukkit.createInventory(h,54,"§8§lMETRO VICE §8| §aGALLERY");h.inventory(inv);
        int slot=0;for(ItemStack item:p.getInventory().getContents())if(isPhoto(item)&&slot<45)inv.setItem(slot++,item.clone());
        if(slot==0)inv.setItem(22,button(Material.MAP,"&7لا توجد لقطات","&7استخدم كاميرا الموقع أولًا."));
        inv.setItem(49,button(Material.ARROW,"&eرجوع للهاتف"));p.openInventory(inv);
    }
    private void openSocial(Player p){
        DeviceMenuHolder h=new DeviceMenuHolder("social");Inventory inv=Bukkit.createInventory(h,27,"§8§lMETRO VICE §8| §dM SOCIAL");h.inventory(inv);
        int slot=0;synchronized(feed){for(String post:feed){if(slot>=9)break;inv.setItem(slot++,button(Material.PAPER,"&d"+post,"&7منشور حديث"));}}
        inv.setItem(13,button(Material.WRITABLE_BOOK,"&aإنشاء منشور","&7شارك تحديثًا مع حاملي الهاتف"));
        inv.setItem(22,button(Material.ARROW,"&eرجوع"));p.openInventory(inv);
    }
    private void openSettings(Player p){
        DeviceMenuHolder h=new DeviceMenuHolder("settings");Inventory inv=Bukkit.createInventory(h,27,"§8§lMETRO VICE §8| §7SETTINGS");h.inventory(inv);
        boolean enabled=notifications.getOrDefault(p.getUniqueId(),true);
        inv.setItem(11,button(Material.BELL,"&eالإشعارات","&7الحالة: "+(enabled?"§aمفعّلة":"§cمتوقفة"),"&7اضغط للتبديل"));
        inv.setItem(13,button(Material.TRIPWIRE_HOOK,"&bرمز قفل الهاتف","&7"+(pins.containsKey(p.getUniqueId())?"تم تعيين رمز لهذه الجلسة":"لا يوجد رمز"),"&7اضغط للتعيين أو الإلغاء"));
        inv.setItem(22,button(Material.ARROW,"&eرجوع"));p.openInventory(inv);
    }
    @EventHandler public void onClick(InventoryClickEvent e){
        if(!(e.getView().getTopInventory().getHolder() instanceof DeviceMenuHolder h))return;
        e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||e.getClickedInventory()!=e.getView().getTopInventory())return;
        int slot=e.getRawSlot();
        switch(h.view()){
            case "phone" -> {if(slot==22){p.closeInventory();return;}switch(slot){
                case 10->ask(p,Input.MESSAGE,"&bاكتب: اسم اللاعب: نص الرسالة");
                case 11->identity(p);case 12->bank(p);case 13->photo(p);case 14->openGallery(p);case 15->openSocial(p);case 16->openSettings(p);default->{}}}
            case "radio" -> {if(slot==22){p.closeInventory();return;}if(slot==13)ask(p,Input.SET_CHANNEL,"&9اكتب التردد من 1 إلى 100، ويمكن استخدام منزلتين عشريتين مثل 5.5 أو 10.25:");}
            case "gallery" -> {if(slot==49)openPhone(p);else if(isPhoto(e.getCurrentItem()))p.sendMessage(MetroViceRP.color("&eتفاصيل اللقطة ظاهرة في وصف العنصر."));}
            case "social" -> {if(slot==22)openPhone(p);else if(slot==13)ask(p,Input.POST,"&dاكتب منشورك على M (حتى 120 حرفًا):");}
            case "settings" -> {if(slot==11){notifications.put(p.getUniqueId(),!notifications.getOrDefault(p.getUniqueId(),true));openSettings(p);}else if(slot==13)ask(p,Input.SET_PIN,"&7اكتب رمزًا من 4 أرقام أو OFF لإلغاء الرمز:");else if(slot==22)openPhone(p);}
            case "phoneinfo" -> {if(slot==22)openPhone(p);}
        }
    }
    private void identity(Player p){
        try{
            CharacterProfile c=characters.active(p.getUniqueId()).orElse(null);
            if(c==null){openInfo(p,"§bبطاقة الهوية","§cلا توجد شخصية نشطة.");return;}
            openInfo(p,"§bبطاقة الهوية",
                    "§fالاسم: §b"+c.name(),
                    "§fالعمر: §b"+c.age(),
                    "§fرقم الهوية: §bMV-"+c.id(),
                    "§fالوظيفة: §b"+c.job(),
                    "§fالرتبة: §b"+c.rank());
        }catch(SQLException ex){plugin.getLogger().warning("Phone identity lookup failed: "+ex.getMessage());openInfo(p,"§bبطاقة الهوية","§cتعذر تحميل الهوية.");}
    }
    private void bank(Player p){
        try{
            CharacterProfile c=characters.active(p.getUniqueId()).orElse(null);
            if(c==null){openInfo(p,"§6المحفظة","§cفعّل شخصية أولًا.");return;}
            openInfo(p,"§6المحفظة",
                    "§fالنقد: §a$"+c.cash(),
                    "§fرصيد البنك: §b$"+c.bank(),
                    "§7الإيداع والسحب من موظف البنك.");
        }catch(SQLException ex){plugin.getLogger().warning("Phone balance lookup failed: "+ex.getMessage());openInfo(p,"§6المحفظة","§cتعذر تحميل الرصيد.");}
    }
    private void openInfo(Player p,String title,String... lines){
        DeviceMenuHolder h=new DeviceMenuHolder("phoneinfo");
        Inventory inv=Bukkit.createInventory(h,27,"§1METRO VICE §8| "+title);h.inventory(inv);
        int slot=10;
        for(String line:lines){
            if(slot>16)break;
            inv.setItem(slot++,button(Material.PAPER,"&f"+line,"&7بيانات محفوظة في ملف شخصيتك"));
        }
        inv.setItem(22,button(Material.ARROW,"&eرجوع للهاتف"));
        p.openInventory(inv);
    }
    private void photo(Player p){
        ItemStack item=new ItemStack(Material.FILLED_MAP);ItemMeta meta=item.getItemMeta();meta.setDisplayName(MetroViceRP.color("&eلقطة MetroCam"));
        meta.setLore(java.util.List.of(MetroViceRP.color("&7العالم: "+p.getWorld().getName()),
            MetroViceRP.color("&7الإحداثيات: "+p.getLocation().getBlockX()+", "+p.getLocation().getBlockY()+", "+p.getLocation().getBlockZ()),
            MetroViceRP.color("&8"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))));
        meta.getPersistentDataContainer().set(photoKey,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);
        if(!p.getInventory().addItem(item).isEmpty()){p.sendMessage(MetroViceRP.color("&cالمخزون ممتلئ."));return;}
        p.sendMessage(MetroViceRP.color("&aتم حفظ لقطة موقعك في المعرض."));openGallery(p);
    }
    private boolean isPhoto(ItemStack i){return i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(photoKey,PersistentDataType.BYTE);}
    private void ask(Player p,Input mode,String prompt){waiting.put(p.getUniqueId(),mode);p.closeInventory();p.sendMessage(MetroViceRP.color(prompt));p.sendMessage(MetroViceRP.color("&8اكتب !cancel للإلغاء."));}
    @EventHandler public void onChat(AsyncPlayerChatEvent e){
        Input mode=waiting.remove(e.getPlayer().getUniqueId());if(mode==null)return;e.setCancelled(true);
        String value=e.getMessage().strip();Player p=e.getPlayer();Bukkit.getScheduler().runTask(plugin,()->processInput(p,mode,value));
    }
    private void processInput(Player p,Input mode,String value){
        if(value.equalsIgnoreCase("!cancel")){p.sendMessage(MetroViceRP.color("&7تم الإلغاء."));openPhone(p);return;}
        switch(mode){
            case MESSAGE->{int sep=value.indexOf(':');if(sep<1||sep==value.length()-1){p.sendMessage(MetroViceRP.color("&cالصيغة: الاسم: الرسالة"));return;}
                Player target=Bukkit.getPlayerExact(value.substring(0,sep).strip());String msg=value.substring(sep+1).strip();
                if(target==null||msg.isEmpty()){p.sendMessage(MetroViceRP.color("&cاللاعب غير متصل أو الرسالة فارغة."));return;}
                p.sendMessage(MetroViceRP.color("&d[رسالة إلى "+target.getName()+"] &f"+msg));
                if(notifications.getOrDefault(target.getUniqueId(),true))target.sendMessage(MetroViceRP.color("&d[MetroPhone من "+p.getName()+"] &f"+msg));}
            case POST->{if(value.isBlank()||value.length()>120){p.sendMessage(MetroViceRP.color("&cالمنشور من 1 إلى 120 حرفًا."));return;}
                String post=p.getName()+": "+value;synchronized(feed){feed.addFirst(post);while(feed.size()>30)feed.removeLast();}
                for(Player online:Bukkit.getOnlinePlayers())if(hasPhone(online)&&notifications.getOrDefault(online.getUniqueId(),true))online.sendMessage(MetroViceRP.color("&d[M] &f"+post));
                p.sendMessage(MetroViceRP.color("&aتم نشر تحديثك على M."));}
            case SET_PIN->{if(value.equalsIgnoreCase("OFF")){pins.remove(p.getUniqueId());p.sendMessage(MetroViceRP.color("&aتم إلغاء الرمز."));}
                else if(value.matches("\\d{4}")){pins.put(p.getUniqueId(),value);p.sendMessage(MetroViceRP.color("&aتم تعيين رمز الهاتف لهذه الجلسة."));}
                else p.sendMessage(MetroViceRP.color("&cاكتب 4 أرقام أو OFF."));}
            case UNLOCK->{if(value.equals(pins.get(p.getUniqueId())))openPhone(p);else p.sendMessage(MetroViceRP.color("&cرمز القفل غير صحيح."));}
            case SET_CHANNEL->{
                try{
                    double frequency=Double.parseDouble(value);
                    if(!Double.isFinite(frequency)||frequency<1.0||frequency>100.0||Math.abs(frequency*100.0-Math.rint(frequency*100.0))>0.000001){
                        p.sendMessage(MetroViceRP.color("&cالتردد يجب أن يكون بين 1 و100 وبحد أقصى منزلتين عشريتين."));
                        openRadio(p);return;
                    }
                    if(!canUseFrequency(p,frequency)){
                        p.sendMessage(MetroViceRP.color("&cلا تملك صلاحية دخول هذا التردد. الإسعاف مسموح له بالتردد 5 فقط."));
                        openRadio(p);return;
                    }
                    channels.put(p.getUniqueId(),frequency);
                    p.sendMessage(MetroViceRP.color("&aتم ضبط الراديو على التردد &b"+formatFrequency(frequency)));
                    openRadio(p);
                }catch(NumberFormatException ex){p.sendMessage(MetroViceRP.color("&cأدخل رقم تردد صحيح مثل 5.5 أو 10.25."));openRadio(p);}
            }}
        }
    private boolean hasPhone(Player p){for(ItemStack i:p.getInventory().getContents())if(i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(phoneKey,PersistentDataType.BYTE))return true;return false;}
    public boolean sendRadioMessage(Player sender,String message){
        if(!hasRadio(sender)){sender.sendMessage(MetroViceRP.color("&cيجب أن تحمل الراديو لإرسال الرسائل."));return true;}
        if(message==null||message.isBlank()){sender.sendMessage(MetroViceRP.color("&eالاستخدام: /radio <رسالتك>"));return true;}
        double frequency=channels.getOrDefault(sender.getUniqueId(),1.0);
        if(!canUseFrequency(sender,frequency)){sender.sendMessage(MetroViceRP.color("&cلا تملك صلاحية استخدام هذا التردد."));return true;}
        String shown=formatFrequency(frequency);
        String line=MetroViceRP.color("&9[لاسلكي "+shown+"] &b"+sender.getName()+"&f: "+message);
        for(Player target:Bukkit.getOnlinePlayers()){
            double targetFrequency=channels.getOrDefault(target.getUniqueId(),1.0);
            if(hasRadio(target)&&Double.compare(targetFrequency,frequency)==0&&canUseFrequency(target,frequency))target.sendMessage(line);
        }
        return true;
    }
    private boolean canUseFrequency(Player player,double frequency){
        try{
            var memberships=departments.memberships(player.getUniqueId());
            boolean police=memberships.stream().anyMatch(m->m.department().equals("POLICE"));
            boolean ems=memberships.stream().anyMatch(m->m.department().equals("EMS"));
            if(police)return true;
            return ems&&Math.abs(frequency-5.0)<0.000001;
        }catch(SQLException ex){
            plugin.getLogger().warning("Radio permission lookup failed: "+ex.getMessage());
            return false;
        }
    }
    private String formatFrequency(double frequency){
        return java.math.BigDecimal.valueOf(frequency).stripTrailingZeros().toPlainString();
    }
    private boolean hasRadio(Player p){if(isRadio(p.getInventory().getItemInMainHand())||isRadio(p.getInventory().getItemInOffHand()))return true;for(ItemStack i:p.getInventory().getContents())if(isRadio(i))return true;return false;}
    private boolean isRadio(ItemStack i){return i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(radioKey,PersistentDataType.BYTE);}
    private ItemStack button(Material material,String name,String... lore){ItemStack i=new ItemStack(material);ItemMeta m=i.getItemMeta();m.setDisplayName(MetroViceRP.color(name));m.setLore(java.util.Arrays.stream(lore).map(MetroViceRP::color).toList());i.setItemMeta(m);return i;}
}