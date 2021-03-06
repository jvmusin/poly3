package retrofit

import okhttp3.Response

fun Response.bufferedBody() = if (body == null) null else peekBody(Long.MAX_VALUE)
