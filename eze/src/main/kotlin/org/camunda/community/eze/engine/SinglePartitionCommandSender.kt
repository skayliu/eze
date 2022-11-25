/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package org.camunda.community.eze.engine

import io.camunda.zeebe.engine.api.InterPartitionCommandSender
import io.camunda.zeebe.logstreams.log.LogStreamRecordWriter
import io.camunda.zeebe.protocol.impl.record.RecordMetadata
import io.camunda.zeebe.protocol.record.RecordType
import io.camunda.zeebe.protocol.record.ValueType
import io.camunda.zeebe.protocol.record.intent.Intent
import io.camunda.zeebe.util.buffer.BufferWriter
import java.util.function.Consumer

class SinglePartitionCommandSender(
    private val writerLookUp: (Int) -> LogStreamRecordWriter
) : InterPartitionCommandSender {

    private fun withRecordWriter(
        receiverPartitionId: Int,
        writer: Consumer<LogStreamRecordWriter>
    ) {
        val recordWriter = writerLookUp(receiverPartitionId)
        writer.accept(recordWriter)
        recordWriter.tryWrite()
    }

    override fun sendCommand(
        receiverPartitionId: Int,
        valueType: ValueType,
        intent: Intent,
        command: BufferWriter
    ) {
        withRecordWriter(receiverPartitionId) { writer ->
            val recordMetadata =
                RecordMetadata()
                    .recordType(RecordType.COMMAND)
                    .valueType(valueType)
                    .intent(intent)

            writer
                .metadataWriter(recordMetadata)
                .valueWriter(command)
        }
    }

    override fun sendCommand(
        receiverPartitionId: Int,
        valueType: ValueType,
        intent: Intent,
        recordKey: Long,
        command: BufferWriter
    ) {
        withRecordWriter(receiverPartitionId) { writer ->
            val recordMetadata =
                RecordMetadata()
                    .recordType(RecordType.COMMAND)
                    .valueType(valueType)
                    .intent(intent)

            writer
                .key(recordKey)
                .metadataWriter(recordMetadata)
                .valueWriter(command)
        }
    }
}
