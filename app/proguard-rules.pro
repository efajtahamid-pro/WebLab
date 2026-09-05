# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep data model classes used for reflection-based persistence
-keep class com.efajtahamid.weblab.data.db.** { *; }
-keep class com.efajtahamid.weblab.data.model.** { *; }
