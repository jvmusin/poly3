package bacs

import org.koin.dsl.module

@Suppress("USELESS_CAST")
val bacsModule = module {
    single<BacsArchiveService> { BacsArchiveServiceFactory().create() }
}