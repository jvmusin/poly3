package sybon

import org.koin.dsl.module
import org.koin.experimental.builder.singleBy
import sybon.api.SybonApiFactory

val sybonModule = module {
    single { SybonApiFactory(get()) }
    single { get<SybonApiFactory>().createArchiveApi() }
    single { get<SybonApiFactory>().createCheckingApi() }

    singleBy<SybonService, SybonServiceImpl>()
}