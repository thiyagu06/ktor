/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.engine

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

/**
 * Base class for implementing [ApplicationEngine]
 *
 * It creates default engine pipeline, provides [application] property and installs default transformations
 * on respond and receive
 *
 * @param environment instance of [ApplicationEngineEnvironment] for this engine
 * @param pipeline pipeline to use with this engine
 */
@EngineAPI
public abstract class BaseApplicationEngine(
    public final override val environment: ApplicationEngineEnvironment,
    public val pipeline: EnginePipeline = defaultEnginePipeline(environment)
) : ApplicationEngine {

    /**
     * Configuration for the [BaseApplicationEngine]
     */
    public open class Configuration : ApplicationEngine.Configuration()

    init {
        BaseApplicationResponse.setupSendPipeline(pipeline.sendPipeline)
        environment.monitor.subscribe(ApplicationStarting) {
            it.receivePipeline.merge(pipeline.receivePipeline)
            it.sendPipeline.merge(pipeline.sendPipeline)
            it.receivePipeline.installDefaultTransformations()
            it.sendPipeline.installDefaultTransformations()
            it.installDefaultInterceptors()
        }
        environment.monitor.subscribe(ApplicationStarted) {
            environment.connectors.forEach {
                environment.log.info("Responding at ${it.type.name.toLowerCase()}://${it.host}:${it.port}")
            }
        }
    }

    private fun Application.installDefaultInterceptors() {
        intercept(ApplicationCallPipeline.Fallback) {
            if (call.response.status() == null) {
                val errorStatusCode = call.attributes.getOrNull(RoutingFailureStatusCode) ?: HttpStatusCode.NotFound
                call.respond(errorStatusCode)
            }
        }
    }
}
