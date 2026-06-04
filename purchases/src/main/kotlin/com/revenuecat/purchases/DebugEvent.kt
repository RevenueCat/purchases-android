package com.revenuecat.purchases

@InternalRevenueCatAPI
public class DebugEvent(
    public val name: DebugEventName,
    public val properties: Map<String, String> = emptyMap(),
)
