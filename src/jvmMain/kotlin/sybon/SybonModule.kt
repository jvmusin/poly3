package sybon

import org.koin.dsl.module
import sybon.api.SybonApiFactory

fun sybonModule(config: SybonConfig) = module {
    with(SybonApiFactory(config)) {
        single { createArchiveApi() }
        single { createCheckingApi() }
    }

    single<SybonArchiveService>(MainProblemArchive) { SybonArchiveServiceImpl(get(), MainProblemArchive.collectionId) }
    single<SybonArchiveService>(TestProblemArchive) { SybonArchiveServiceImpl(get(), TestProblemArchive.collectionId) }
    single { SybonCheckingService(get()) }
}
