import io.kotest.core.spec.style.StringSpec
import okhttp3.OkHttpClient
import okhttp3.Request


class SybonWebTests : StringSpec({
    "request a page" {
        val client = OkHttpClient.Builder().build()

        val request = Request.Builder()
            .url("http://web.archive.bacs.cs.istu.ru/")
            .header("Cookie", cookie)
            .get()
            .build()
        println(client.newCall(request).execute().body!!.string())
    }
}) {
    companion object {
        val cookie = """
            .AspNetCore.Antiforgery.JMD10GPAgik=CfDJ8E6GdKgCkBlJhM-bLrGzvd7lWfD6SOOcpNTujZyWoblJ_FzmHdAEx_29O2k6tnP4LOV2nGsP2GjzTX_sXAKZ89v2gFK00AMbj6p4E51IW8oV6xEltvkT8HR4Gd_SPXNDOQn0uXrNW0cTIFpZG4evt0Q; .AspNetCore.Identity.Application=CfDJ8E6GdKgCkBlJhM-bLrGzvd6EzE3Uk8Dix0PmQljGsfhKvYnArThoEIviEb-iG-4yfQl0PpZJDwm9A8I-cs9cL0ydGpegQ8n13sMYp0nHa1KdHy-0K9a1msH-vQ3V2i83P6W1awxdeS0b6J6N3XMR4GJ5gdlSlH1Kbzy0WlWOpevFteVIutCfNyKiV8XHj0bugAUPgN88LVz4VB-ThI98uhrLyOmTuLRpsHBymI7WQJl4a0T3z0O1zS7J7QDLcYImNedj6TMGz5391FgS5Ry2cX540P9fte1iOjbTE8H6EhJeQGIMtTyeWpgRqYrFUEYI0wXct-fBTvPNg8lNsGsLZATghQM29iJURIwSx_1UvYL8C6TBbY1cAyLyX1txO2At5gPV0CzOqvH3lxJhUrrZBiuXHq9tYhlkrWRQaiPdK4u0k6J2O1FYQsAClBS3De5rk1HV78vxv0tPkpub3DK9Uxx4LhzxiicyRN0186tyNyrxBZoki7nPJVQRlD9UMUIuYIhOk9bIDfGlEpv2fivbTu80lLJ0OTPi88iMtFsDTaC-5jrHqfTsOcpfOQzwZrwLLG5LFtved8ulBGBQt4EG1nq_xIX4zvOYvuyb6_JeRgk_8kK_o6OC-TcO2PR-_to27LgFquO94uU4IqQkIUHOi-jYoPRvkCLRyxDpCoIPyjK7lqXylaaMEQEE4hUEtVbb7AM9t7z5Szoa1Nws5yQWWJI_mHI77f6HRegMSJxA6tYF0Xjr-opHjOq40EzByKYz23V30GqrA_EzwxEzKJq6qnd-uIaNZxk48yz0S_HxdFa3Ix86Z9lSrChR7JPkYgu6jw
        """.trimIndent()
    }
}