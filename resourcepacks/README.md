# Metro Vice RP Resource Packs

The Java and Bedrock packs include dedicated MetroPhone and radio item artwork.

**Compatibility note:** these first item textures replace the vanilla clock and goat-horn icons globally. Bukkit persistent item tags are not automatically translated by Geyser into separate Bedrock custom items. The phone and radio menus themselves use standard inventory interfaces for cross-play compatibility.

## Phone features
- Messages to online players (live delivery, not message history).
- Character identity and current cash/bank balance.
- Location snapshot cards saved as map items (not screenshots of the game view).
- M social feed (session memory; resets on restart).
- Notification toggle and a four-digit phone PIN (session-only).

## Radio
- Five selectable text channels.
- Players need to carry a radio item to transmit and receive.
- Voice push-to-talk is not included.

## Server delivery
The plugin's resource-pack sender requires a direct HTTPS file URL and the matching SHA-1. Upload the generated Java ZIP or Bedrock MCpack to a direct HTTPS host, then set `resource-pack.enabled: true`, `resource-pack.url`, and `resource-pack.sha1` in `plugins/MetroViceRP/config.yml`. A GitHub Actions artifact download page is not itself a direct resource-pack URL.


## تحديث 0.8.2
- واجهة الراديو تسمح بإدخال تردد من 1 إلى 100 حتى منزلتين عشريتين.
- الشرطة المسجلة بالقسم يمكنها استخدام الترددات كلها، والإسعاف المسجل بالقسم يمكنه استخدام التردد 5 فقط.
- سجل الشرطة يعرض جميع الموظفين المسجلين في قاعدة البيانات، مع حالة المناوبة، ويعيد تحديث نفسه أثناء فتحه.
- هذه النسخة لا تدّعي أن تسجيل البصمة الفعلي أو واجهة الراديو الصوتية قد اكتملت؛ حالة المناوبة الحالية مرتبطة بنظام المناوبة الموجود.
