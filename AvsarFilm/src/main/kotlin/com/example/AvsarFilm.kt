package com.example

import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

/**
 * Avşar Film'in resmi YouTube kanalını (UCcpY8WNM1xHPb8t2rjjZmwg) listeler.
 *
 * Video indirmez ve kopyalamaz: yalnızca kanalın herkese açık RSS beslemesini
 * okur, oynatmayı CloudStream'in YouTube çıkartıcısına bırakır.
 *
 * SINIRLAMA (sürüm 1): YouTube RSS beslemesi kanalın yalnızca en son ~15 videosunu verir.
 */
class AvsarFilm : MainAPI() {
    override var mainUrl = "https://www.youtube.com"
    override var name = "Avşar Film"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    private val channelId = "UCcpY8WNM1xHPb8t2rjjZmwg"
    private val feedUrl = "$mainUrl/feeds/videos.xml?channel_id=$channelId"

    override val mainPage = mainPageOf(
        feedUrl to "Son Yüklenenler"
    )

    private data class Video(
        val id: String,
        val title: String,
        val poster: String?,
        val description: String?
    ) {
        val watchUrl get() = "https://www.youtube.com/watch?v=$id"
    }

    private fun String.xmlDecode(): String = this
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    private suspend fun fetchVideos(): List<Video> {
        val xml = app.get(feedUrl).text
        return xml.split("<entry>").drop(1).mapNotNull { entry ->
            val id = Regex("<yt:videoId>(.*?)</yt:videoId>").find(entry)?.groupValues?.get(1)
                ?: return@mapNotNull null
            val title = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
                .find(entry)?.groupValues?.get(1)?.xmlDecode()?.trim()
                ?: return@mapNotNull null
            val poster = Regex("<media:thumbnail url=\"(.*?)\"").find(entry)?.groupValues?.get(1)
            val description = Regex(
                "<media:description>(.*?)</media:description>",
                RegexOption.DOT_MATCHES_ALL
            ).find(entry)?.groupValues?.get(1)?.xmlDecode()?.trim()
            Video(id, title, poster, description)
        }
    }

    private suspend fun Video.toSearchResponse(): SearchResponse =
        newMovieSearchResponse(title, watchUrl, TvType.Movie) {
            this.posterUrl = poster
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // RSS beslemesinde sayfalama yok; yalnızca ilk sayfa dolu döner.
        if (page > 1) return newHomePageResponse(request.name, emptyList(), false)
        val items = fetchVideos().map { it.toSearchResponse() }
        return newHomePageResponse(request.name, items, false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return fetchVideos()
            .filter { it.title.contains(query, ignoreCase = true) }
            .map { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.get(1) ?: return null
        val video = fetchVideos().firstOrNull { it.id == id }
        val title = video?.title ?: "Avşar Film"
        val poster = video?.poster ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        val plot = video?.description
        val watchUrl = "https://www.youtube.com/watch?v=$id"

        return newMovieLoadResponse(title, watchUrl, TvType.Movie, watchUrl) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Oynatma CloudStream'in yerleşik YouTube çıkartıcısı tarafından yapılır.
        return loadExtractor(data, subtitleCallback, callback)
    }
}
