package sybon

import org.koin.dsl.module
import org.koin.experimental.builder.singleBy
import sybon.api.SybonApiFactory

val sybonModule = module {
    SybonApiFactory().run {
        single { createArchiveApi() }
        single { createCheckingApi() }
    }

    singleBy<SybonService, SybonServiceImpl>()
    singleBy<SybonArchiveBuilder, SybonArchiveBuilderImpl>()
}