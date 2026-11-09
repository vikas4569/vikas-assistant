# Vikas Assistant (Android, Kotlin)
Setup: Android Studio -> Open -> is folder ko select karo -> Sync -> phone par Run.
1. App kholo, saari permissions Allow karo.
2. "Accessibility ON karo" -> Vikas Assistant -> ON (sirf app close ke liye).
Commands (Hinglish/Hindi/English): kholo | band karo | call karo | message <naam> <text> | search <text>
Security: koi internet permission nahi, koi data store nahi, allowBackup=false, service exported=false,
accessibility sirf com.android.settings par, call/SMS/close se pehle confirmation.

## APK kaise banaye (bina Android Studio)
GitHub par naya repo -> ye saari files push karo -> Actions tab -> "Build APK" -> artifact "VikasAssistant-APK" download karo -> phone mein install (Unknown sources allow).
