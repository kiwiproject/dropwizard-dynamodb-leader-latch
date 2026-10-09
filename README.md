### Dropwizard DynamoDB Leader Latch

[![Build](https://github.com/kiwiproject/dropwizard-dynamodb-leader-latch/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/kiwiproject/dropwizard-dynamodb-leader-latch/actions/workflows/build.yml?query=branch%3Amain)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_dropwizard-dynamodb-leader-latch&metric=alert_status)](https://sonarcloud.io/dashboard?id=kiwiproject_dropwizard-dynamodb-leader-latch)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_dropwizard-dynamodb-leader-latch&metric=coverage)](https://sonarcloud.io/dashboard?id=kiwiproject_dropwizard-dynamodb-leader-latch)
[![CodeQL](https://github.com/kiwiproject/dropwizard-dynamodb-leader-latch/actions/workflows/codeql.yml/badge.svg)](https://github.com/kiwiproject/dropwizard-dynamodb-leader-latch/actions/workflows/codeql.yml)
[![javadoc](https://javadoc.io/badge2/org.kiwiproject/dropwizard-dynamodb-leader-latch/javadoc.svg)](https://javadoc.io/doc/org.kiwiproject/dropwizard-dynamodb-leader-latch)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/org.kiwiproject/dropwizard-dynamodb-leader-latch)](https://central.sonatype.com/artifact/org.kiwiproject/dropwizard-dynamodb-leader-latch/)

This is a small library that integrates the DynamoDB-backed leader latch from
[dynamodb-leader-latch](https://github.com/kiwiproject/dynamodb-leader-latch) into a Dropwizard service.
It is the DynamoDB counterpart to
[dropwizard-leader-latch](https://github.com/kiwiproject/dropwizard-leader-latch), which uses Apache
Curator and ZooKeeper.

## Usage

Add the dependency (this brings in `dynamodb-leader-latch` and its AWS SDK dependencies):

```xml
<dependency>
    <groupId>org.kiwiproject</groupId>
    <artifactId>dropwizard-dynamodb-leader-latch</artifactId>
    <version>[current-version]</version>
</dependency>
```

Then, in your Dropwizard `Application.run` method:

```java
// You own the DynamoDbClient and must close it; the latch never closes it.
// DynamoDbClient.create() uses the default region and credentials (the task role on ECS).
var dynamoDb = DynamoDbClient.create();

// Register closing the client BEFORE starting the latch. Dropwizard stops managed objects in reverse order, so the
// latch stops (and releases the lock) first, and the client is closed after it.
environment.lifecycle().manage(new Managed() {
    @Override
    public void stop() {
        dynamoDb.close();
    }
});

var configuration = LeaderLatchConfiguration.forTable("service-leader-locks");

var serviceDescriptor = ServiceDescriptor.builder()
        .name("order-service")
        .version("1.2.3")
        .hostname(hostname)
        .port(port)
        .build();

var leaderLatch = ManagedLeaderLatchCreator.startLeaderLatch(
        dynamoDb, configuration, environment, serviceDescriptor, listeners);

if (leaderLatch.hasLeadership()) {
    // leader-only work
}
```

`ManagedLeaderLatchCreator` creates a `ManagedLeaderLatch`, tells Dropwizard to manage (stop) it, starts it, and
registers a health check and two REST resources. Starting never waits to become the leader. If the latch cannot be
started at all, a `ManagedLeaderLatchException` is thrown so the service does not start half-working; any later problem,
such as DynamoDB being unreachable, is reported as a value instead of an exception (see
[dynamodb-leader-latch](https://github.com/kiwiproject/dynamodb-leader-latch) for the table schema, IAM permissions,
and configuration). Add listeners before starting, either as arguments or with `addLeaderLatchListener`.

You must close the `DynamoDbClient` yourself, as the example does with a `Managed` registered before the latch is
started: the latch never closes it, and an unclosed client keeps its connections and threads alive. See "Configuring the
`DynamoDbClient`" in [dynamodb-leader-latch](https://github.com/kiwiproject/dynamodb-leader-latch) for custom CA
certificates and timeouts.

Use `ManagedLeaderLatchCreator.from(...)` to configure the creator first (`withoutHealthCheck()`,
`withoutResources()`, `addLeaderLatchListener(...)`) and then call `start()`.

### Endpoints

| Endpoint | Response |
|---|---|
| `GET /kiwi/got-leader-latch` | `204 No Content`; present only when the service participates in a leader latch |
| `GET /kiwi/leader-latch/leader` | `{"leader": true}` or `{"leader": false}` |
| `GET /kiwi/leader-latch/latch` | `id`, `leader`, `leadershipKey`, `leaderId` (the participant DynamoDB records as leader, or null), and `status` (`IsLeader`, `NotLeader`, `NotStarted`, `Closed`, or `Uncertain`) |

The `/latch` endpoint reads the lock record from DynamoDB.

### Health check

Registered as `leaderLatch`. It is unhealthy (CRITICAL) when the latch is not started or is closed, when the leader
cannot be read from DynamoDB, when there is no leader, and when this instance believes it is the leader but DynamoDB
records another participant. It reads the lock record (one `GetItem`) each time it runs. A monitor that checks every
instance of a service should also flag more than one instance reporting that it is the leader.

## Migrating from dropwizard-leader-latch

The class names are the same, in the package `org.kiwiproject.dynamodb.leader.dropwizard` instead of
`org.kiwiproject.curator.leader`:

| Before | After |
|---|---|
| depends on `dropwizard-leader-latch` (Curator and ZooKeeper) | depends on `dropwizard-dynamodb-leader-latch` |
| `ManagedLeaderLatchCreator.startLeaderLatch(curatorClient, environment, descriptor, listeners)` | `ManagedLeaderLatchCreator.startLeaderLatch(dynamoDbClient, configuration, environment, descriptor, listeners)` |
| Curator's `LeaderLatchListener` | `org.kiwiproject.dynamodb.leader.LeaderLatchListener` (same two methods, `isLeader()` and `notLeader()`) |
| `hasLeadership()` could throw | never throws; use `checkLeadershipStatus()` to tell "not leader" from "cannot tell" |
| `whenLeader(...)` returned an `Optional` | returns a `WhenLeaderResult`: `RanAsLeader`, `SkippedNotLeader`, or `ActionFailed` |
| `getParticipants()` | removed; use `getLeader()` for the current leader |
| `/kiwi/leader-latch/latch` returned `latchPath`, `participants`, `state` | returns `leadershipKey`, `leaderId`, `status` |
