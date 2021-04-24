package bacs

import org.koin.dsl.module
import java.nio.file.Files

fun bacsModule(config: BacsConfig) = module {
    single { BacsArchiveBuilder(Files.createTempDirectory("bacs-archive-builder")) }
    single { BacsArchiveServiceFactory(config, get()).create() }
}
