import bacs.BacsConfig
import bacs.bacsModule
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import polygon.PolygonConfig
import polygon.polygonModule

val config: Config = ConfigFactory.parseResources("application.conf").resolve()
val polygonConfig: PolygonConfig = config.extract("polygon")
val bacsConfig: BacsConfig = config.extract("bacs")

val bacsModule = bacsModule(bacsConfig)
val polygonModule = polygonModule(polygonConfig)
