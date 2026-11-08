import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Where the ffmpeg **pack** comes from: the sibling `ffmpeg-build` checkout's local build,
 * `../ffmpeg-build/out/<slug>/pack`. Nothing is downloaded — a missing pack fails the build with
 * the script that produces it.
 *
 * A pack is a flat directory: the CLI (`ffmpeg` / `ffmpeg.exe` / Android `libffmpeg.so`) plus the
 * same `libav*` / `libsw*` DSOs MSE loads. Rpath is baked (`@loader_path` / `$ORIGIN`); do not
 * paste `LD_LIBRARY_PATH` when exec-ing the CLI.
 *
 * Consumers that exec the CLI pack the whole directory: the Android apk (per ABI under
 * `lib/<abi>/`) and the desktop app (compose app-resources). Dev `:desktopApp:run` / tests point at
 * the staged host CLI via `-Dio.ymusic.ffmpeg`.
 */
object FfmpegBuild {
    /** The ABIs the apk and the device tests ship; every Android native (quickjs, mse, ffmpeg) is built for each. */
    val ANDROID_ABIS = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

    /**
     * ABIs to stage/pack for this Gradle invocation. Default = [ANDROID_ABIS].
     * Override with `-PandroidAbis=arm64-v8a` (comma-separated) for a single-device debug APK
     * when only some sibling ffmpeg-build packs exist (e.g. Galaxy arm64).
     */
    fun androidAbis(project: Project): List<String> {
        val raw = project.findProperty("androidAbis")?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) return ANDROID_ABIS
        val requested = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val unknown = requested.filter { it !in ANDROID_ABIS }
        require(unknown.isEmpty()) { "unknown -PandroidAbis values: $unknown (allowed: $ANDROID_ABIS)" }
        require(requested.isNotEmpty()) { "-PandroidAbis must list at least one ABI" }
        return requested
    }

    fun root(project: Project): File = project.rootProject.projectDir.parentFile.resolve("ffmpeg-build")

    /** ffmpeg-build out/ slug for this machine (`macos-x86_64`, `linux-arm64`, …), or null. */
    fun hostSlug(): String? {
        val os = System.getProperty("os.name").lowercase()
        val arm = System.getProperty("os.arch").lowercase() in setOf("aarch64", "arm64")
        return when {
            os.contains("mac") -> if (arm) "macos-arm64" else "macos-x86_64"
            os.contains("linux") -> if (arm) "linux-arm64" else "linux-x86_64"
            os.contains("win") -> if (arm) "windows-arm64" else "windows-x86_64"
            else -> null
        }
    }

    fun androidSlug(abi: String): String = "android-$abi"

    /** Staged per consumer, so the two sync tasks never write to the same directory. */
    fun hostDir(project: Project): File = File(project.rootProject.layout.buildDirectory.get().asFile, "ffmpeg/host")

    fun androidDir(project: Project): File =
        File(project.rootProject.layout.buildDirectory.get().asFile, "ffmpeg/android")

    fun androidPackDir(project: Project, abi: String): File = File(androidDir(project), abi)

    fun hostCliName(): String =
        if (System.getProperty("os.name").lowercase().contains("win")) "ffmpeg.exe" else "ffmpeg"

    /** The host CLI after a sync, or null on an OS ffmpeg-build has no target for. */
    fun hostBinary(project: Project): File? = hostSlug()?.let { File(hostDir(project), hostCliName()) }

    fun androidCli(project: Project, abi: String): File = File(androidPackDir(project, abi), "libffmpeg.so")

    fun pack(project: Project, slug: String): File = File(root(project), "out/$slug/pack")

    /** The ffmpeg-build script invocation that produces [slug]'s pack. */
    fun buildCommand(slug: String): String = when {
        slug.startsWith("android-") -> "scripts/build-android.sh ${slug.removePrefix("android-")}"
        slug.startsWith("ios-") -> "scripts/build-ios.sh"
        else -> "scripts/build-desktop.sh"
    }

    /** ffmpeg-build prefix (`include/` + `lib/`) MSE CMake links against, if present. */
    fun localBuildPrefix(project: Project, slug: String): File? {
        val prefix = File(root(project), "out/$slug")
        return prefix.takeIf { File(it, "include").isDirectory && File(it, "lib").isDirectory }
    }

    fun hostPrefix(project: Project): File? = hostSlug()?.let { localBuildPrefix(project, it) }
}

/**
 * Stages sibling `ffmpeg-build/out/<slug>/pack` directories into [outputDir]: each value of [packs]
 * is the slug, each key the subdirectory it lands in (`""` = the output root). A rebuild in
 * ffmpeg-build changes the input files and re-stages.
 */
abstract class SyncFfmpeg : DefaultTask() {
    /** Destination subdirectory → ffmpeg-build slug. */
    @get:Input abstract val packs: MapProperty<String, String>

    @get:Input abstract val ffmpegBuildRoot: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packFiles: ConfigurableFileCollection

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun sync() {
        val out = outputDir.get().asFile.apply { mkdirs() }
        val root = File(ffmpegBuildRoot.get())
        for ((subdir, slug) in packs.get()) {
            val pack = File(root, "out/$slug/pack")
            check(packLooksPresent(pack)) {
                "no ffmpeg pack for $slug at $pack — build it in ffmpeg-build: ${FfmpegBuild.buildCommand(slug)}"
            }
            copyPack(pack, if (subdir.isEmpty()) out else File(out, subdir))
            logger.info("ffmpeg: $slug <- ${pack.absolutePath}")
        }
    }

    private fun packLooksPresent(dir: File): Boolean =
        File(dir, "ffmpeg").isFile || File(dir, "ffmpeg.exe").isFile || File(dir, "libffmpeg.so").isFile

    private fun copyPack(src: File, dest: File) {
        dest.mkdirs()
        dest.listFiles()?.forEach { it.deleteRecursively() }
        // Prefer `cp -a` so Mach-O symlinks stay links (Java copyRecursively would dereference
        // them and triple the pack). On Windows Gradle often has no `cp` on PATH; desktop packs
        // there are flat DLL files, so a plain recursive copy is fine.
        if (!copyPackWithCp(src, dest)) {
            src.listFiles()?.forEach { child ->
                child.copyRecursively(File(dest, child.name), overwrite = true)
            }
        }
        listOf("ffmpeg", "ffmpeg.exe", "libffmpeg.so").forEach { name ->
            File(dest, name).takeIf { it.isFile }?.setExecutable(true)
        }
    }

    private fun copyPackWithCp(src: File, dest: File): Boolean {
        val candidates = listOf(
            "cp",
            "C:/msys64/usr/bin/cp.exe",
            "C:/msys64/usr/bin/cp",
        )
        for (cp in candidates) {
            try {
                val process = ProcessBuilder(cp, "-a", "${src.absolutePath}/.", dest.absolutePath)
                    .redirectErrorStream(true).start()
                val text = process.inputStream.bufferedReader().readText()
                check(process.waitFor() == 0) { "cp -a $src -> $dest failed: $text" }
                return true
            } catch (_: java.io.IOException) {
                // try next candidate
            }
        }
        return false
    }
}
