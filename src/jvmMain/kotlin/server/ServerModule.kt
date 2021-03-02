package server

import org.koin.dsl.module
import org.koin.experimental.builder.singleBy

val serverModule = module {
    singleBy<MessageSenderFactory, MessageSenderFactoryImpl>()
}
