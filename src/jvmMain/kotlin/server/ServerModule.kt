package server

import org.koin.dsl.module

val serverModule = module { single { MessageSenderFactory() } }