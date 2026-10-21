package com.revenuecat.purchases.iam

import android.util.Base64
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * A fake RevenueCat backend for the IAM end-to-end suite. It issues real-looking tokens, resolves the user
 * of each IAM-path request from its Bearer token, and records every request. Tests script failures, holds and
 * one-off responses on top.
 */
internal class FakeRevenueCatBackend : Dispatcher() {

    data class Request(
        val method: String,
        val path: String,
        val authorization: String?,
        val body: JSONObject?,
        val eTag: String?,
        val idempotencyKey: String?,
    ) {
        /** The access token this request carried, or null for the API key / no header. */
        val bearerToken: String?
            get() = authorization?.removePrefix("Bearer ")?.takeIf { it.count { c -> c == '.' } == 2 }
    }

    private val recorded = CopyOnWriteArrayList<Request>()
    private val arrivals = LinkedBlockingQueue<Request>()
    private val tokenOwners = ConcurrentHashMap<String, String>()
    private val refreshOwners = ConcurrentHashMap<String, String>()
    private val rejectedAccessTokens = ConcurrentHashMap.newKeySet<String>()
    private val oneOffResponses = ConcurrentHashMap<String, MutableList<() -> MockResponse>>()
    private val holds = ConcurrentHashMap<String, CountDownLatch>()
    private val subscriptions = ConcurrentHashMap<String, MutableSet<String>>()
    private val balances = ConcurrentHashMap<String, MutableMap<String, Int>>()
    private val spendsByKey = ConcurrentHashMap<String, () -> MockResponse>()
    private val serial = AtomicInteger()

    /** Whether an anonymous login adopts the `app_user_id` it's sent (iOS #8017 behavior). */
    @Volatile
    var adoptsAnonymousAppUserID: Boolean = true

    /** The app user ID the next non-anonymous login resolves to. */
    @Volatile
    var identifiedAppUserID: String = "identified-user"

    /** Every token set the backend has issued, in order. */
    val issuedTokens = CopyOnWriteArrayList<JSONObject>()

    val requests: List<Request>
        get() = recorded.toList()

    fun requests(path: String): List<Request> = recorded.filter { it.path == path }

    /** Blocks for at most [timeoutMs] until another request arrives; false if none did. */
    fun awaitArrival(timeoutMs: Long): Boolean = arrivals.poll(timeoutMs, TimeUnit.MILLISECONDS) != null

    fun ownerOf(accessToken: String?): String? = accessToken?.let { tokenOwners[it] }

    /** The next request to [path] gets [response] instead of the default one. */
    fun respondOnce(path: String, response: () -> MockResponse) {
        oneOffResponses.getOrPut(path) { CopyOnWriteArrayList() }.add(response)
    }

    /** Requests to [path] block until [release] is called, so a test can act while they're in flight. */
    fun hold(path: String) {
        holds[path] = CountDownLatch(1)
    }

    fun release(path: String) {
        holds.remove(path)?.countDown()
    }

    fun setBalance(appUserID: String, code: String, balance: Int) {
        balances.getOrPut(appUserID) { ConcurrentHashMap() }[code] = balance
    }

    fun balance(appUserID: String, code: String): Int? = balances[appUserID]?.get(code)

    /** Requests carrying [accessToken] get a 401 from now on, as if it expired. */
    fun expire(accessToken: String) {
        rejectedAccessTokens.add(accessToken)
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.requestUrl?.encodedPath ?: request.path.orEmpty()
        val body = request.body.readUtf8().takeIf { it.isNotBlank() }
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        val recordedRequest = Request(
            request.method.orEmpty(),
            path,
            request.getHeader("Authorization"),
            body,
            request.getHeader(ETAG_HEADER),
            request.getHeader(IDEMPOTENCY_KEY_HEADER),
        )
        recorded.add(recordedRequest)
        try {
            holds[path]?.await(HOLD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            oneOffResponses[path]?.let { queue -> if (queue.isNotEmpty()) return queue.removeAt(0)() }
            return route(recordedRequest)
        } finally {
            arrivals.add(recordedRequest)
        }
    }

    private fun route(request: Request): MockResponse {
        val token = request.bearerToken
        return when {
            token != null && token in rejectedAccessTokens -> unauthorized()
            request.path.startsWith("/auth/") -> authRoute(request.path, request.body ?: JSONObject())
            else -> apiRoute(request, ownerOf(token))
        }
    }

    private fun authRoute(path: String, body: JSONObject): MockResponse = when (path) {
        "/auth/login" -> logIn(body)
        "/auth/token" -> refresh(body)
        "/auth/revoke" -> json("{}")
        else -> notFound()
    }

    // IAM paths resolve the user from the token; legacy paths carry it in the URL or body.
    private fun apiRoute(request: Request, tokenOwner: String?): MockResponse {
        val path = request.path
        return when {
            path == "/v1/customer" -> tokenOwner?.let(::customerInfo) ?: unauthorized()
            path == "/v1/subscribers/identify" ->
                customerInfo(request.body?.optString("new_app_user_id")).setResponseCode(CREATED)
            path.startsWith("/v1/subscribers/") && path.count { it == '/' } == LEGACY_CUSTOMER_SLASHES ->
                customerInfo(URLDecoder.decode(path.substringAfterLast('/'), "UTF-8"))
            path == "/v1/customer/virtual_currencies" ->
                tokenOwner?.let { json(virtualCurrencies(it)) } ?: unauthorized()
            path == "/v1/customer/virtual_currencies/spend" -> tokenOwner?.let { spend(it, request) } ?: unauthorized()
            path == "/v1/receipts" -> postReceipt(tokenOwner ?: request.body?.optString("app_user_id"), request.body)
            path.endsWith("/offerings") -> json(OFFERINGS)
            path == "/v1/product_entitlement_mapping" -> json("""{"product_entitlement_mapping":{}}""")
            path.endsWith("/attributes") -> json("{}")
            else -> notFound()
        }
    }

    private fun logIn(body: JSONObject): MockResponse {
        val method = body.optString("method")
        val appUserID = when {
            method != "anonymous" -> identifiedAppUserID
            adoptsAnonymousAppUserID && body.has("app_user_id") -> body.getString("app_user_id")
            else -> "server-anonymous-${serial.incrementAndGet()}"
        }
        val amr = if (method == "anonymous") listOf("anonymous") else listOf("anonymous", method)
        return json(tokensFor(appUserID, amr).toString())
    }

    private fun refresh(body: JSONObject): MockResponse {
        val owner = refreshOwners[body.optString("refresh_token")] ?: return unauthorized()
        return json(tokensFor(owner, listOf("anonymous")).toString())
    }

    fun tokensFor(appUserID: String, amr: List<String>): JSONObject {
        val n = serial.incrementAndGet()
        val access = jwt(JSONObject().put("rc.app_user_id", appUserID).put("n", n))
        val refresh = "refresh-$appUserID-$n"
        tokenOwners[access] = appUserID
        refreshOwners[refresh] = appUserID
        return JSONObject()
            .put("access_token", access)
            .put("refresh_token", refresh)
            .put("id_token", jwt(JSONObject().put("amr", JSONArray(amr)).put("n", n)))
            .also { issuedTokens.add(it) }
    }

    // Every posted product becomes an active subscription of that user, so a test can tell the post landed.
    private fun postReceipt(appUserID: String?, body: JSONObject?): MockResponse {
        val productIDs = body?.optJSONArray("product_ids") ?: JSONArray()
        val owned = subscriptions.getOrPut(appUserID.orEmpty()) { ConcurrentHashMap.newKeySet() }
        for (index in 0 until productIDs.length()) owned.add(productIDs.getString(index))
        return customerInfo(appUserID)
    }

    // Like the real backend, a repeated Idempotency-Key gets the first response and is applied once.
    @Synchronized
    private fun spend(appUserID: String, request: Request): MockResponse {
        val key = request.idempotencyKey
            ?: return MockResponse().setResponseCode(BAD_REQUEST).setBody("""{"code":7000,"message":"No key"}""")
        return spendsByKey.getOrPut(key) { applySpend(appUserID, request.body) }()
    }

    private fun applySpend(appUserID: String, body: JSONObject?): () -> MockResponse {
        val adjustments = body?.optJSONObject("adjustments") ?: JSONObject()
        val codes = adjustments.keys().asSequence().toList()
        val owned = balances.getOrPut(appUserID) { ConcurrentHashMap() }
        if (codes.any { (owned[it] ?: 0) < adjustments.getInt(it) }) {
            return {
                MockResponse().setResponseCode(UNPROCESSABLE)
                    .setBody("""{"code":7000,"message":"Insufficient balance"}""")
                    .setHeader("Content-Type", "application/json")
            }
        }
        codes.forEach { owned[it] = owned.getValue(it) - adjustments.getInt(it) }
        val response = virtualCurrencies(appUserID)
        return { json(response) }
    }

    private fun virtualCurrencies(appUserID: String): String = JSONObject().put(
        "virtual_currencies",
        JSONObject().apply {
            balances[appUserID].orEmpty().forEach { (code, balance) ->
                put(code, JSONObject().put("balance", balance).put("code", code).put("name", code))
            }
        },
    ).toString()

    private fun customerInfo(appUserID: String?): MockResponse = customerInfoBody(appUserID)
        .setHeader(ETAG_HEADER, "etag-$appUserID")

    private fun customerInfoBody(appUserID: String?): MockResponse = json(
        JSONObject()
            .put("request_date", "2026-10-19T10:00:00Z")
            .put("request_date_ms", REQUEST_DATE_MS)
            .put(
                "subscriber",
                JSONObject()
                    .put("entitlements", JSONObject())
                    .put("first_seen", "2026-10-01T10:00:00Z")
                    .put("non_subscriptions", JSONObject())
                    .put("original_app_user_id", appUserID.orEmpty())
                    .put("original_application_version", "1.0")
                    .put("other_purchases", JSONObject())
                    .put("subscriptions", subscriptionsOf(appUserID))
                    .put("management_url", JSONObject.NULL),
            )
            .toString(),
    )

    private fun subscriptionsOf(appUserID: String?) = JSONObject().apply {
        subscriptions[appUserID.orEmpty()].orEmpty().forEach { productID ->
            put(
                productID,
                JSONObject()
                    .put("purchase_date", "2026-10-19T09:00:00Z")
                    .put("expires_date", "2099-01-01T00:00:00Z")
                    .put("period_type", "normal")
                    .put("store", "play_store")
                    .put("is_sandbox", true)
                    .put("unsubscribe_detected_at", JSONObject.NULL)
                    .put("billing_issues_detected_at", JSONObject.NULL),
            )
        }
    }

    private fun json(body: String) = MockResponse().setResponseCode(OK).setBody(body)
        .setHeader("Content-Type", "application/json")

    private fun notFound() = MockResponse()
        .setResponseCode(NOT_FOUND)
        .setBody("""{"code":7000,"message":"Not found"}""")

    // The real backend always sends an error body; a bodyless 401 never reaches the refresh check.
    private fun unauthorized() = MockResponse().setResponseCode(UNAUTHORIZED)
        .setBody("""{"code":7225,"message":"Invalid access token"}""")
        .setHeader("Content-Type", "application/json")

    companion object {
        private const val OK = 200
        private const val CREATED = 201
        private const val BAD_REQUEST = 400
        private const val UNAUTHORIZED = 401
        private const val UNPROCESSABLE = 422
        private const val NOT_FOUND = 404
        private const val HOLD_TIMEOUT_SECONDS = 20L
        private const val LEGACY_CUSTOMER_SLASHES = 3
        private const val REQUEST_DATE_MS = 1_792_000_000_000L
        const val ETAG_HEADER = "X-RevenueCat-ETag"
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

        // One offering with the product FakeBilling stubs, so offerings calls succeed.
        private val OFFERINGS = JSONObject()
            .put("current_offering_id", "default")
            .put(
                "offerings",
                JSONArray().put(
                    JSONObject()
                        .put("identifier", "default")
                        .put("description", "Default")
                        .put(
                            "packages",
                            JSONArray().put(
                                JSONObject()
                                    .put("identifier", "\$rc_monthly")
                                    .put("platform_product_identifier", "monthly")
                                    .put("platform_product_plan_identifier", "monthly_base_plan"),
                            ),
                        ),
                ),
            )
            .toString()

        // Unsigned; the SDK only reads claims.
        fun jwt(payload: JSONObject): String =
            listOf("""{"alg":"none"}""", payload.toString(), "").joinToString(".") {
                Base64.encodeToString(it.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            }
    }
}
