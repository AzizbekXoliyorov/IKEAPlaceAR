# IKEA Place AR

Android uchun kengaytirilgan reallik (AR) ilovasi: mebelni real xonada 3D ko‘rinishda joylashtirish.
Paket: `com.azizbek.ikeaplace` · Til: Kotlin · UI: Jetpack Compose · AR: Google ARCore · 3D: SceneView (Filament)

## Topshiriq maqsadi
IKEA Place ilovasiga o‘xshash AR ilova yaratish: kamera orqali haqiqiy muhitni ko‘rsatish, gorizontal yuzalarni aniqlash,
3D mebel modellarini yuzaga joylashtirish va boshqarish, natijani skrinshot sifatida saqlash.

## Texnologiyalar
Kotlin 2.0, Jetpack Compose (Material 3), ARCore, SceneView `arsceneview 2.2.1`, GLB/glTF 2.0, Gradle Kotlin DSL (AGP 8.7, Gradle 8.9), GitHub Actions, MediaStore.

## Arxitektura
- `Catalog.kt` — 10 ta mebel (nom, GLB yo‘li, miniatyura, real o‘lchamlar).
- `MainActivity.kt` — ekranlar orasida navigatsiya (Home, Catalog, Ar, Preview, Gallery, Settings).
- `ui/ArScreen.kt` — ARCore tekshiruvi, ruxsatlar, `ARScene`, hit-test, anchor boshqaruvi, boshqaruv paneli.
- `ui/Screens.kt` — qolgan ekranlar. `ScreenshotSaver.kt` — PixelCopy + MediaStore.
- `assets/models/*.glb` — ilova ichiga joylangan modellar (tashqi URL ishlatilmaydi).
- `tools/generate_models.py` — GLB modellar va miniatyuralarni yaratuvchi skript.

## AR ishlash prinsipi
ARCore kamera tasviri va qurilma harakat sensorlari (SLAM) asosida muhitning xususiyat nuqtalarini kuzatadi va qurilmaning
fazodagi holatini hisoblaydi. SceneView shu holatni virtual kameraga uzatadi, shuning uchun 3D obyektlar kamera harakatlanganda ham haqiqiy joyida turadi.

## Gorizontal yuzalarni aniqlash
`Config.PlaneFindingMode.HORIZONTAL` yoqilgan. Topilgan tekisliklar `planeRenderer` orqali setka ko‘rinishida chiziladi.
Yuza topilmaguncha yuqorida “Yuzani qidirish…” yozuvi ko‘rinadi.

## 3D modelni joylashtirish
Foydalanuvchi pastdagi lentadan mebelni tanlaydi va ekranga tegadi. `frame.hitTest()` natijasidan faqat yuqoriga qaragan gorizontal
tekislik ichidagi nuqta olinadi, undan `Anchor` yaratiladi, `AnchorNode` ostiga `ModelNode` qo‘shiladi. Bir vaqtda 20 tagacha obyekt mumkin.

## Obyektni boshqarish
- Tanlash: obyektga teging. Surish, ikki barmoq bilan masshtab va burish: SceneView `isEditable` imo-ishoralari (yangi anchor yaratilmaydi).
- Tugmalar: Ko‘chirish (yangi joyga tegib qayta anchor qilinadi), Burish (15°), +/− masshtab, Nusxa, Asl holat, O‘chirish.

## Skrinshot
Kamera tugmasi interfeysni vaqtincha yashiradi, `PixelCopy` bilan kamera + 3D tasvir olinadi, ko‘rib chiqish ekrani ochiladi.
“Saqlash” bosilganda `MediaStore` orqali `Pictures/IKEAPlaceAR` ga yoziladi. Android 9 va undan pastda `WRITE_EXTERNAL_STORAGE` so‘raladi, 10+ da ruxsat kerak emas.

## Minimal talablar
Android 8.0 (API 26)+, ARCore qo‘llab-quvvatlaydigan qurilma va Google Play Services for AR. Emulyatorda to‘liq ishlamasligi mumkin — haqiqiy telefon tavsiya etiladi.

## APK o‘rnatish
1. GitHub → Actions → oxirgi muvaffaqiyatli ishga tushirish → Artifacts → `app-debug-apk` ni yuklab oling va zipdan chiqaring.
2. `app-debug.apk` ni telefonga o‘tkazing, “Noma’lum manbalardan o‘rnatish”ga ruxsat bering va oching.
3. Yoki USB orqali: `adb install app-debug.apk`.

## GitHub Actions orqali yig‘ish
1. Loyihani GitHub repozitoriyga `main` shoxiga push qiling (yoki Actions → Android Build → Run workflow).
2. Workflow: JDK 17, Android SDK, litsenziyalar, Gradle kesh, kompilyatsiya, unit testlar, `assembleDebug`.
3. Natija: `app/build/outputs/apk/debug/app-debug.apk` (artifact `app-debug-apk`).
4. Release APK uchun secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Lokal: `./gradlew :app:assembleDebug`

## Hisobot uchun skrinshotlar (5–7 ta) va video
Skrinshotlar: (1) bosh ekran; (2) katalog; (3) yuza aniqlanishi (setka); (4) mebel joylashtirilgan; (5) bir nechta obyekt; (6) boshqaruv paneli (burish/masshtab); (7) galereya yoki saqlangan rasm.
Telefonda tizim skrinshoti: quvvat + ovozni pasaytirish tugmalari.

Video (2–3 daqiqa): tizim ekran yozuvini yoqing (tez sozlamalar → “Ekranni yozish”). Ketma-ketlik: ilovani ochish (10s) → katalog (15s) → xonani skanerlash (20s) → joylashtirish (30s) →
ko‘chirish/burish/masshtab (40s) → nusxa va o‘chirish (20s) → skrinshot va galereya (25s). Fayl nomi: `ikea_place_demo.mp4`.
