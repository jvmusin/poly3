package polygon

import org.koin.dsl.module
import org.koin.experimental.builder.singleBy

fun polygonModule(polygonConfig: PolygonConfig) = module {
    single { PolygonApiFactory(polygonConfig).create() }
    singleBy<PolygonService, PolygonServiceImpl>()
}
