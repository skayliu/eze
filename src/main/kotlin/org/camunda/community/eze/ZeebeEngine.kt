package org.camunda.community.eze

import io.camunda.zeebe.protocol.record.Record

interface ZeebeEngine {
    fun start()
    fun stop()

    fun records(): Iterable<Record<*>>
}
