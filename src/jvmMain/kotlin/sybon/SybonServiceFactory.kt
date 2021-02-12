package sybon

class SybonServiceFactory(private val sybonApiFactory: SybonApiFactory) {
    fun create() = SybonService(sybonApiFactory.createArchiveApi(), sybonApiFactory.createCheckingApi())
}