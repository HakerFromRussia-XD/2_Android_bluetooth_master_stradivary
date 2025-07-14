import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllOptions(
    @SerialName("GAME_LAUNCH_RATE")
    var gameLaunchRate: String? = null,
    @SerialName("MAXIMUM_POINTS")
    var maximumPoints: String? = null,
    @SerialName("NUMBER_OF_CUPS")
    var numberOfCups: String? = null
    // Если понадобится, можно добавить другие поля
)

@Serializable
data class TestModel(
    @SerialName("settings")
    var allOptions: String? = null
)

@Serializable
data class Token(
    val token: String
)