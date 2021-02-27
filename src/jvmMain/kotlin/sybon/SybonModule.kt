package sybon

import org.koin.dsl.module
import org.koin.experimental.builder.singleBy
import sybon.api.SybonApiFactory

val sybonModule = module {
    with(SybonApiFactory()) {
        single { createArchiveApi() }
        single { createCheckingApi() }
    }

    single<SybonArchiveService>(MainProblemArchive) { SybonArchiveServiceImpl(get(), MainProblemArchive.collectionId) }
    single<SybonArchiveService>(TestProblemArchive) { SybonArchiveServiceImpl(get(), TestProblemArchive.collectionId) }
    singleBy<SybonCheckingService, SybonCheckingServiceImpl>()
}