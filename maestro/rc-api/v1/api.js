for (var EXPECTED_PARAMS = ['apiKey'], i = 0; i < EXPECTED_PARAMS.length; i++) {
  if (!eval(EXPECTED_PARAMS[i])) throw new Error(EXPECTED_PARAMS[i] + ' is not provided');
}

function revokeGooglePlaySubscription(appUserId, productId) {
  return http.post(
    'https://api.revenuecat.com/v1/subscribers/' +
      encodeURIComponent(appUserId) +
      '/subscriptions/' +
      encodeURIComponent(productId) +
      '/revoke', {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + apiKey,
        },
        body: ''
      });
}

output.rcApiV1 = {
  revokeGooglePlaySubscription: revokeGooglePlaySubscription
}
