package com.revenuecat.purchases;

import java.util.Map;

class HTTPClient {
    class Result {
        final Integer responseCode;
        final Map body;

        Result(Integer responseCode, Map body) {
            this.responseCode = responseCode;
            this.body = body;
        }
    }

    private class HTTPErrorException extends Exception {}

    public Result performRequest(final String path,
                                 final Map body,
                                 final Map<String, String> headers)
            throws HTTPErrorException {
        throw new UnsupportedOperationException();
    }
}
