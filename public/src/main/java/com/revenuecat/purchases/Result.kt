package com.revenuecat.purchases

sealed class Result<out A, out B> {
    class Success<A>(val value: A) : Result<A, Nothing>()
    class Error<B>(val value: B) : Result<Nothing, B>()
}
