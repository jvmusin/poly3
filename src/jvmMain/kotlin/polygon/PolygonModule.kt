package polygon

import org.koin.dsl.module
import org.koin.experimental.builder.singleBy
import polygon.api.PolygonApiFactory

/**
 * Creates Polygon module.
 *
 * @param polygonConfig Polygon API configuration properties.
 */
fun polygonModule(polygonConfig: PolygonConfig) = module {
    single { PolygonApiFactory(polygonConfig).create() }
    singleBy<PolygonService, PolygonServiceImpl>()
}
