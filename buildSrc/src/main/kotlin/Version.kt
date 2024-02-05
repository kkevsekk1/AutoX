import com.google.gson.*
import org.gradle.internal.impldep.com.google.gson.annotations.SerializedName
import java.io.File

lateinit var versions: Versions
    private set

const val kotlin_version = "1.6.21"
const val compose_version = "1.2.0-rc01"

fun initVersions(file: File) {
    val json = file.readText()
    versions = Gson().fromJson(json, Versions::class.java)
    println(GsonBuilder().setPrettyPrinting().create().toJson(versions))
}

data class Versions(
    @SerializedName("appVersionCode")
    val appVersionCode: Int = 634,
    @SerializedName("appVersionName")
    val appVersionName: String = "6.3.4",
    @SerializedName("buildTool")
    val buildTool: String = "34.0.0",
    @SerializedName("compile")
    val compile: Int = 34,
    @SerializedName("devVersionCode")
    val devVersionCode: Int = 634,
    @SerializedName("devVersionName")
    val devVersionName: String = "6.3.4",
    @SerializedName("IDE")
    val ide: String = "Android Studio Hedgehog | 2023.1.1",
    @SerializedName("JDK")
    val jdk: String = "17",
    @SerializedName("mini")
    val mini: Int = 21,
    @SerializedName("target")
    val target: Int = 26
)
