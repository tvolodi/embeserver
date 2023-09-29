package kz.ascoa.embeserver.Obsolete

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation


// Delete on release test stage if not used.
//fun Application.module() {
//    install(ContentNegotiation){
//        json()
//    }
//    configureRouting()
//}