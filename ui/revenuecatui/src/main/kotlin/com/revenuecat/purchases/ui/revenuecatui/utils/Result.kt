package com.revenuecat.purchases.ui.revenuecatui.utils

internal sealed class Result<out A, out B> {
    class Success<A>(val value: A) : Result<A, Nothing>()
    class Error<B>(val value: B) : Result<Nothing, B>()
}
