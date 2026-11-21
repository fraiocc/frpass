package cc.fraio.frpass.api

/** Entry point for accessing the active [FrPassAPI] instance. */
object FrPassProvider {

    @Volatile
    private var api: FrPassAPI? = null

    @JvmStatic
    fun isAvailable(): Boolean = api != null

    @JvmStatic
    fun get(): FrPassAPI {
        return api ?: throw IllegalStateException("FrPassAPI is not initialized yet!")
    }

    @JvmStatic
    fun register(api: FrPassAPI) {
        this.api = api
    }

    internal fun unregister() {
        this.api = null
    }
}
