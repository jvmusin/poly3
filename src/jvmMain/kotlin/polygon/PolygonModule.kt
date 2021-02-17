package polygon

import org.koin.dsl.module
import org.koin.experimental.builder.singleBy

val polygonModule = module {
    single { PolygonApiFactory(get()).create() }
    singleBy<PolygonService, PolygonServiceImpl>()
}