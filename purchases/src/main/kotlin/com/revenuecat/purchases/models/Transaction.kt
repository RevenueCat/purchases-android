package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.JsonTools.json
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.responses.SubscriptionInfoResponse
import com.revenuecat.purchases.utils.getDate
import com.revenuecat.purchases.utils.optDate
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.Date
import java.util.Locale

@Parcelize
@Poko
public class Transaction(
    public val transactionIdentifier: String,
    @Deprecated(
        "Use transactionIdentifier instead",
        ReplaceWith("transactionIdentifier"),
    )
    public val revenuecatId: String,
    public val productIdentifier: String,
    @Deprecated(
        "Use productIdentifier instead",
        ReplaceWith("productIdentifier"),
    )
    public val productId: String,
    public val purchaseDate: Date,
    public val storeTransactionId: String?,
    public val store: Store,
    public val displayName: String?,
    public val isSandbox: Boolean = false,
    public val originalPurchaseDate: Date?,
    public val price: Price?,
) : Parcelable {

    @Deprecated(
        message = """
            Use the constructor with all fields instead. This constructor is missing the new fields: displayName, 
            isSandbox, originalPurchaseDate, and price
            """,
        replaceWith = ReplaceWith(
            "Transaction(transactionIdentifier, revenuecatId, productIdentifier, productId, purchaseDate, " +
                "storeTransactionId, store, displayName, isSandbox, originalPurchaseDate, price)",
        ),
    )
    constructor(
        transactionIdentifier: String,
        revenuecatId: String,
        productIdentifier: String,
        productId: String,
        purchaseDate: Date,
        storeTransactionId: String?,
        store: Store,
    ) : this(
        transactionIdentifier = transactionIdentifier,
        revenuecatId = revenuecatId,
        productIdentifier = productIdentifier,
        productId = productId,
        purchaseDate = purchaseDate,
        storeTransactionId = storeTransactionId,
        store = store,
        displayName = null,
        isSandbox = false,
        originalPurchaseDate = null,
        price = null,
    )

    internal constructor(
        productId: String,
        jsonObject: JSONObject,
        locale: Locale = Locale.getDefault(),
    ) : this(
        transactionIdentifier = jsonObject.getString("id"),
        revenuecatId = jsonObject.getString("id"),
        productIdentifier = productId,
        productId = productId,
        purchaseDate = jsonObject.getDate("purchase_date"),
        storeTransactionId = jsonObject.optString("store_transaction_id").takeIf {
            it.isNotBlank()
        },
        store = jsonObject.getString("store").let { Store.fromString(it) },
        displayName = jsonObject.optString("display_name").takeIf {
            it.isNotBlank()
        },
        isSandbox = jsonObject.optBoolean("is_sandbox", false),
        originalPurchaseDate = jsonObject.optDate("original_purchase_date"),
        // Using the PriceResponse class to parse the price JSON object to make it easier to migrate
        // to Kotlin serialization in the future
        price = jsonObject.optJSONObject("price")?.toString()?.let {
            json.decodeFromString<SubscriptionInfoResponse.PriceResponse>(it).toPrice(locale)
        },
    )
}
