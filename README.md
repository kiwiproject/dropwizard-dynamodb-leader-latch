### Dropwizard DynamoDB Leader Latch

[![Build](https://github.com/kiwiproject/dropwizard-dynamodb-leader-latch/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/kiwiproject/dropwizard-dynamodb-leader-latch/actions/workflows/build.yml?query=branch%3Amain)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_dropwizard-dynamodb-leader-latch&metric=alert_status)](https://sonarcloud.io/dashboard?id=kiwiproject_dropwizard-dynamodb-leader-latch)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_dropwizard-dynamodb-leader-latch&metric=coverage)](https://sonarcloud.io/dashboard?id=kiwiproject_dropwizard-dynamodb-leader-latch)
[![CodeQL](https://github.com/kiwiproject/dropwizard-dynamodb-leader-latch/actions/workflows/codeql.yml/badge.svg)](https://github.com/kiwiproject/dropwizard-dynamodb-leader-latch/actions/workflows/codeql.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

This is a small library that integrates the DynamoDB-backed leader latch from
[dynamodb-leader-latch](https://github.com/kiwiproject/dynamodb-leader-latch) into a Dropwizard service.
It is the DynamoDB counterpart to
[dropwizard-leader-latch](https://github.com/kiwiproject/dropwizard-leader-latch), which uses Apache
Curator and ZooKeeper.

> Status: under development. Not yet released.
