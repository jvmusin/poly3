import bacs.bacsModule
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import polygon.polygonModule
import sybon.sybonModule

val config: Config = ConfigFactory.load()
val polygonModule get() = polygonModule(config.extract("polygon"))
val bacsModule get() = bacsModule(config.extract("bacs"))
val sybonModule get() = sybonModule(config.extract("sybon"))
