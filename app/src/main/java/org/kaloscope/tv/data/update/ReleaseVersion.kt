package org.kaloscope.tv.data.update

internal data class ReleaseVersion(
    val major: Long,
    val minor: Long,
    val patch: Long,
) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)

    companion object {
        // Release tags and APK names follow the stable v<major>.<minor>.<patch> workflow.
        private val pattern = Regex("^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$")

        fun parse(value: String): ReleaseVersion? {
            val parts = pattern.matchEntire(value)?.groupValues ?: return null
            return ReleaseVersion(
                parts[1].toLongOrNull() ?: return null,
                parts[2].toLongOrNull() ?: return null,
                parts[3].toLongOrNull() ?: return null,
            )
        }
    }
}
