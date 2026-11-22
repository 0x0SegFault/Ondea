package app.ondea.data

import kotlinx.serialization.json.Json

/** The one JSON format for app data and backups, shared by Android and iOS. */
object AppDataJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: AppData): ByteArray = json.encodeToString(AppData.serializer(), data).encodeToByteArray()

    fun decode(bytes: ByteArray): AppData = json.decodeFromString(AppData.serializer(), bytes.decodeToString())
}
