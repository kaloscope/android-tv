# Project-specific R8 rules belong here.
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget**
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild

# AkDanmaku creates ECS systems and components through reflection and relies on
# their class identity. Version 1.0.4 ships no consumer rules, so R8 otherwise
# removes constructors and merges classes used only by the runtime engine.
-keep class com.kuaishou.akdanmaku.** { *; }

# Retrofit reflects this nested suspend return type, while callers only inspect
# whether its envelope data is null. Keep the DTO so R8 cannot rewrite it to Any.
-keep,allowoptimization,allowobfuscation class org.kaloscope.tv.data.search.remote.IndexerAuthData
