package com.revenuecat.purchases.common.security

/**
 * Derives the [EncryptedItemStorage] password from the SDK's configured API key.
 *
 * ## Rationale
 *
 * The API key is used directly as the PBKDF2 password material — [EncryptedItemStorage.create]
 * already performs the actual key-stretching (100,000 PBKDF2 iterations), so this function
 * deliberately does not add a second round of hashing on top of it; two independent,
 * hard-to-audit derivation steps doing similar work would only make the scheme harder to reason
 * about, not more secure. Deriving from the API key — rather than a device identifier or a
 * separately-persisted random secret — means the password is deterministically re-derivable any
 * time the SDK is configured with the same key: nothing new needs to be persisted, and the value
 * survives app reinstall / backup-restore automatically as long as the same API key is used to
 * configure the SDK again.
 *
 * ## Trade-off
 *
 * An API key is not a high-entropy secret, and it ships inside the app binary — anyone who can
 * extract it can also derive this storage's encryption key. This is judged acceptable because the
 * data this storage protects (IAM access/refresh/ID tokens) is short-lived and revocable, and the
 * actual threat this storage defends against is another app or process on the same device reading
 * the raw ciphertext file directly — not a determined attacker who already has more direct API
 * access via the key itself.
 *
 * @param apiKey the SDK's configured API key (`PurchasesConfiguration.apiKey`)
 * @return a [CharArray] suitable for use as the `password` argument to
 *   [EncryptedItemStorage.create]. The caller is responsible for zeroing this array once it has
 *   been consumed.
 * @throws IllegalArgumentException if [apiKey] is blank
 */
@Throws(IllegalArgumentException::class)
internal fun derivePassword(apiKey: String): CharArray {
    require(apiKey.isNotBlank()) { "Cannot derive a storage password from a blank API key." }
    return apiKey.toCharArray()
}
