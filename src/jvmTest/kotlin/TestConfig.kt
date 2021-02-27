import bacs.BacsConfig
import bacs.bacsModule
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import polygon.PolygonConfig
import polygon.polygonModule
import sybon.SybonConfig
import sybon.sybonModule

val config: Config = ConfigFactory.load()
val polygonConfig: PolygonConfig = config.extract("polygon")
val bacsConfig: BacsConfig = config.extract("bacs")
val sybonConfig: SybonConfig = config.extract("sybon")

val bacsModule = bacsModule(bacsConfig)
val polygonModule = polygonModule(polygonConfig)
val sybonModule = sybonModule(sybonConfig)
