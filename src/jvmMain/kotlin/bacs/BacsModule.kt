package bacs

import org.koin.dsl.module

fun bacsModule(config: BacsConfig) = module {
    single<BacsArchiveService> { BacsArchiveServiceFactory(config).create() }
}
