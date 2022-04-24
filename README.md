[![Community badge: Incubating](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)
[![Community extension badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)
![Compatible with: Camunda Platform 8](https://img.shields.io/badge/Compatible%20with-Camunda%20Platform%208-0072Ce)

# Embedded Zeebe engine

A lightweight version of [Zeebe](https://github.com/camunda-cloud/zeebe) from [Camunda](https://camunda.com). It bundles Zeebe's workflow engine including some required parts to a library that can be used by other projects. Some parts of Zeebe, like clustering, are not included or are replaced with simpler implementations, for example with an In-Memory database.   

> The project was created as part of the Camunda Summer Hackdays 2021.

**Features:**

* Support Zeebe clients and exporters
* Read and write records from and to an in-memory log stream
* Time-Travel API

## Usage

### Maven 

Add one of the following dependency to your project (i.e. Maven pom.xml):

For the embedded engine:

```
<dependency>
  <groupId>org.camunda.community</groupId>
  <artifactId>eze</artifactId>
  <version>0.3.0</version>
</dependency>
```

For the JUnit5 extension: 

```
<dependency>
  <groupId>org.camunda.community</groupId>
  <artifactId>eze-junit-extension</artifactId>
  <version>0.3.0</version>
</dependency>
```

Note that the library is written in Kotlin. In general, it works for all JVM languages, like Java. However, it may work best with Kotlin. 

#### Bootstrap the Engine

Use the factory to create a new engine.  

```
val engine: ZeebeEngine = EngineFactory.create()
engine.start()

// ...
engine.stop()
```

#### Connect a Zeebe Client

The engine includes an embedded gRPC gateway to support interactions with Zeebe clients. 

Use the factory method of the engine to create a preconfigured Zeebe client. 

```
val client: ZeebeClient = engine.createClient()
```

Or, create your own Zeebe client using the provided gateway address from the engine.

```
val client: ZeebeClient = ZeebeClient.newClientBuilder()
  .gatewayAddress(engine.getGatewayAddress())
  .usePlaintext()
  .build()
```

#### Read Records from the Log Stream

The engine stores all records (i.e. commands, events, rejections) on a append-only log stream.

Use the engine to read the records. It provides different methods based on the value type (e.g. `processRecords()`, `processInstanceRecords()`). 

```                
engine.processRecords().forEach { record ->
    // ...
}

engine.processInstanceRecords()
    .withElementType(BpmnElementType.PROCESS)
    .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATED)
    .forEach { record ->  
        // ...
    }               
```

For testing or debugging, you can print the records. 

```
// print the most relevant parts of the records
engine.records().print(compact = true)

// print as raw JSON records
engine.records().print(compact = false)
```

#### Time Travel

The engine has an internal clock that is used for timer events or other time-dependent operations.

Use the engine clock to manipulate the time.

```
engine.clock().increaseTime(timeToAdd = Duration.ofMinutes(5))
```

#### Exporters

The engine supports Zeebe exporters. An exporter reads the records from the log stream and can export them to an external system.

Use the factory to create a new engine and register exporters.  

```
val engine: ZeebeEngine = EngineFactory.create(exporters = listOf(HazelcastExporter()))
```

#### JUnit5 Extension

The project contains a JUnit5 extension to write tests more smoothly.  

Add the `@EmbeddedZeebeEngine` annotation to your test class. The extension injects fields of the following types to interact with the engine:
* `ZeebeEngine`
* `ZeebeClient`
* `RecordStreamSource`
* `ZeebeEngineClock`

```
@EmbeddedZeebeEngine
class ProcessTest {

  // the extension injects the fields before running the test   
  lateinit var client: ZeebeClient
  lateinit var recordStream: RecordStreamSource
  lateinit var clock: ZeebeEngineClock

  @Test
  fun `should complete process`() {
    // given
    val process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .endEvent()
        .done()

    client.newDeployCommand()
        .addProcessModel(process, "process.bpmn")
        .send()
        .join()

    // when
    val processInstanceResult = client.newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .variables(mapOf("x" to 1))
        .withResult()
        .send()
        .join()

    // then
    assertThat(processInstanceResult.variablesAsMap)
        .containsEntry("x", 1)
  }
```

### Docker

EZE can be run via docker. The benefits over running a normal Zeebe docker image are, the reductions of dependencies and components, which should make start up and execution much more performant.

```sh
docker run -p 26500:26500 -it ghcr.io/camunda-community-hub/eze:0.8.1
```

You can check with zbctl whether it works to access the engine.

```sh
$ zbctl status --insecure --address 0.0.0.0:26500
Cluster size: 1
Partitions count: 1
Replication factor: 1
Gateway version: A.B.C
Brokers:
  Broker 0 - 0.0.0.0:26500
    Version: X.Y.Z
    Partition 1 : Leader, Healthy
```

The usage of the Docker container is currently quite limited, since the client/user has no access to the log. The log is currently not exposed. Furthermore, it is not yet possible to configure an exporter, but hopefully soon.
