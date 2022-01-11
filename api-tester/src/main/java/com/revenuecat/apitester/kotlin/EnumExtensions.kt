package com.revenuecat.apitester.kotlin

/**
 * Usage:
 * when (enum) {
 *  A,
 *  B -> {}
 * }.exhaustive
 * @see https://proandroiddev.com/til-when-is-when-exhaustive-31d69f630a8b
 * The Java equivalent would be using Switch expressions but that requires Java 14.
 */
val <T> T.exhaustive: T
    get() = this
