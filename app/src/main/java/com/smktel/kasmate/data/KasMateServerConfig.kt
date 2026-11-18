package com.smktel.kasmate.data

/** Shared server address defaults for foreground API calls and background push registration. */
object KasMateServerConfig {
    const val DEFAULT_ADDRESS = "10.199.241.182"

    private val previousAddresses = listOf(
        "192.168.18.9",
        "10.210.34.103",
        "10.210.39.103"
    )

    /** Moves saved app addresses from older builds to the current desktop server IP. */
    fun migrateSavedAddress(savedAddress: String?): String {
        if (savedAddress.isNullOrBlank()) return DEFAULT_ADDRESS
        return previousAddresses.fold(savedAddress) { address, previous ->
            address.replace(previous, DEFAULT_ADDRESS)
        }
    }
}
