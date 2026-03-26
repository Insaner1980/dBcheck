# dBcheck ProGuard rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Billing
-keep class com.android.vending.billing.**

# Vico
-keep class com.patrykandpatrick.vico.** { *; }
