package util

import org.koin.dsl.module

val retrofitModule = module {
    single { RetrofitClientFactory() }
}