package io.github.lorber13.jellyfinmusic.core.data.stream

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo

const val TEST_ACCESS_TOKEN = "secret-token"

/** A signed-in client. createJellyfin needs an Android Context, so the SDK's HTTP factory builds it directly. */
fun testApiClient(baseUrl: String = "http://music.local:8096"): ApiClient = OkHttpFactory().let { factory ->
    factory.create(
        baseUrl = baseUrl,
        accessToken = TEST_ACCESS_TOKEN,
        clientInfo = ClientInfo(name = "Jellyfin Music Player", version = "0.1.0"),
        deviceInfo = DeviceInfo(id = "device-1", name = "Pixel 10"),
        httpClientOptions = HttpClientOptions(),
        socketConnectionFactory = factory
    )
}
