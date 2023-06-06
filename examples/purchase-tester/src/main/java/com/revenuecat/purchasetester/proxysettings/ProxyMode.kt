package com.revenuecat.purchasetester.proxysettings

enum class ProxyMode {
    OFF,
    ENTITLEMENT_OVERRIDE,
    SERVER_DOWN,
    ;

    fun requestPath(): String {
        return when (this) {
            OFF -> "off"
            ENTITLEMENT_OVERRIDE -> "entitlements"
            SERVER_DOWN -> "server_down"
        }
    }

    companion object {
        fun fromString(modeName: String): ProxyMode {
            return when (modeName) {
                "OFF" -> OFF
                "OVERRIDE_ENTITLEMENTS" -> ENTITLEMENT_OVERRIDE
                "SERVER_DOWN" -> SERVER_DOWN
                else -> throw IllegalArgumentException("Unknown proxy mode: $modeName")
            }
        }
    }
}
