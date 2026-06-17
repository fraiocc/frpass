package cc.fraio.frpass.api

object FrPassProvider {
    private var api: FrPassAPI? = null

    fun get(): FrPassAPI {
        return api ?: throw IllegalStateException("FrPassAPI is not initialized yet!")
    }

    fun register(api: FrPassAPI) {
        this.api = api
    }
}
