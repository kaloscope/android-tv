# Project-specific R8 rules belong here.
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget**
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild

# Retrofit reflects this nested suspend return type, while callers only inspect
# whether its envelope data is null. Keep the DTO so R8 cannot rewrite it to Any.
-keep,allowoptimization,allowobfuscation class org.kaloscope.tv.data.search.remote.IndexerAuthData
