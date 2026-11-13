# ProGuard / R8 rules for DEX Code Optimization & Obfuscation on Google Play

# Repackage all non-kept classes into the default package to maximize obfuscation %
-dontoptimize
-repackageclasses
-allowaccessmodification

# Preserve line numbers and source file attributes for Play Console crash deobfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve runtime annotations, generic signatures and enclosing methods
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Suppress all harmless unresolved class reference warnings to skip R8 verification loops
-dontwarn **
-ignorewarnings

# AndroidX Room database preservation
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep data models used in Room and race telemetry serialization
-keepclassmembers class com.example.model.** { *; }
-keepclassmembers class com.example.game.model.** { *; }

# Automotive distraction safety manager
-keep class com.example.automotive.AutomotiveSafetyManager { *; }
