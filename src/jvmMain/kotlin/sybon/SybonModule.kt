package sybon

import org.koin.dsl.module
import sybon.api.SybonApiFactory

/** Defines Sybon components. */
fun sybonModule(config: SybonConfig) = module {
    with(SybonApiFactory(config)) {
        single { createArchiveApi() }
        single { createCheckingApi() }
    }

    fun register(archive: ProblemArchive) = single(archive) { SybonArchiveService(get(), archive.collectionId) }
    register(MainProblemArchive)
    register(TestProblemArchive)
    single { SybonCheckingService(get()) }
}
