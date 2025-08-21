# راهنمای اعمال تغییرات در Android Studio

## 🎯 روش‌های مختلف اعمال تغییرات

### روش 1: استفاده از Terminal در Android Studio

1. **باز کردن Terminal**:
   - `View` → `Tool Windows` → `Terminal`
   - یا کلید میانبر: `Alt+F12` (Windows/Linux) یا `Option+F12` (Mac)

2. **دانلود و اجرای اسکریپت**:
```bash
# دانلود فایل‌ها از پروژه دیباگ شده
curl -O https://raw.githubusercontent.com/your-repo/apply_changes.sh
chmod +x apply_changes.sh
./apply_changes.sh
```

### روش 2: استفاده از Gradle Task

1. **کپی کردن فایل**:
   - فایل `apply_fixes.gradle` را در root پروژه قرار دهید

2. **اجرای Task**:
   - `View` → `Tool Windows` → `Gradle`
   - در پنل Gradle: `Tasks` → `custom` → `applyPrayerTimesFixes`
   - یا در Terminal: `./gradlew applyPrayerTimesFixes`

### روش 3: استفاده از Git Patch

1. **ذخیره کردن patch**:
   - فایل `prayer_times_fixes.patch` را در root پروژه قرار دهید

2. **اعمال patch**:
```bash
git apply prayer_times_fixes.patch
```

### روش 4: تغییرات دستی (پیشنهادی)

#### فایل 1: `local.properties`
```properties
# در root پروژه ایجاد کنید
sdk.dir=/path/to/your/Android/Sdk
```

#### فایل 2: `app/src/main/java/com/oqba26/prayertimes/utils/PrayerUtils.kt`
کل محتوا را جایگزین کنید:

```kotlin
package com.oqba26.prayertimes.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.MultiDate
import java.io.InputStreamReader

fun getPrayerTimes(context: Context, date: MultiDate): Map<String, String> {
    try {
        val inputStream = context.assets.open("prayer_times.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        val data: Map<String, Map<String, String>> = Gson().fromJson(reader, type)
        reader.close()
        inputStream.close()
        
        // استخراج ماه و روز از تاریخ شمسی برای جستجو در داده‌های 12 ماهه
        val monthDay = extractMonthDay(date.shamsi)
        return data[monthDay] ?: getDefaultPrayerTimes()
    } catch (e: Exception) {
        e.printStackTrace()
        return getDefaultPrayerTimes()
    }
}

private fun extractMonthDay(shamsiDate: String): String {
    // از تاریخ کامل مثل "1403/01/15" فقط "01/15" رو استخراج می‌کنه
    val parts = shamsiDate.split("/")
    return if (parts.size >= 3) {
        "${parts[1]}/${parts[2]}"
    } else {
        shamsiDate
    }
}

private fun getDefaultPrayerTimes(): Map<String, String> {
    return linkedMapOf(
        "طلوع بامداد" to "05:00",
        "طلوع خورشید" to "06:30",
        "ظهر" to "12:30",
        "عصر" to "16:00",
        "غروب" to "18:30",
        "عشاء" to "20:00"
    )
}
```

#### فایل 3: `app/src/main/AndroidManifest.xml`
دو خط زیر را اضافه کنید:

```xml
<!-- در بخش permissions -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

<!-- در بخش application -->
android:layoutDirection="rtl"
```

#### فایل 4: `app/src/main/assets/prayer_times.json`
ساختار را به این شکل تغییر دهید:

```json
{
  "01/01": {
    "طلوع بامداد": "05:15",
    "طلوع خورشید": "06:45",
    "ظهر": "12:15",
    "عصر": "15:45",
    "غروب": "18:00",
    "عشاء": "19:30"
  }
}
```

## 🔧 مراحل بعد از اعمال تغییرات

### 1. تنظیم Android SDK
در فایل `local.properties`:
```properties
# Windows
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# macOS
sdk.dir=/Users/YourName/Library/Android/sdk

# Linux
sdk.dir=/home/YourName/Android/Sdk
```

### 2. Sync پروژه
- `File` → `Sync Project with Gradle Files`
- یا کلید میانبر: `Ctrl+Shift+O` (Windows/Linux) یا `Cmd+Shift+O` (Mac)

### 3. Clean و Rebuild
```bash
./gradlew clean
./gradlew build
```

### 4. تست اپلیکیشن
- اپ را روی device یا emulator اجرا کنید
- بررسی کنید که اوقات نماز درست نمایش داده می‌شود

## ✅ نکات مهم

1. **فایل JSON**: حتماً فایل کامل خودتان را با ساختار جدید آماده کنید
2. **اسامی اوقات**: دقیقاً از همین اسامی استفاده کنید
3. **Backup**: قبل از تغییرات، یک backup تهیه کنید
4. **Test**: بعد از تغییرات، اپ را تست کنید

## 🆘 در صورت مشکل

اگر با مشکل مواجه شدید:
1. فایل‌های backup را بازگردانید
2. پروژه را Clean کنید
3. Gradle را Sync کنید
4. مجدداً تلاش کنید

موفق باشید! 🚀