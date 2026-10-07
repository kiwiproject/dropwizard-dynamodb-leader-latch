package org.kiwiproject.dynamodb.leader.dropwizard;

import lombok.Builder;
import lombok.Value;

/**
 * Value class that contains metadata about a service that participates in a leader latch.
 * <p>
 * The {@code name} is the leadership key shared by all instances of the same logical service, and the
 * name, version, hostname, and port together identify one participant.
 */
@Value
@Builder
public class ServiceDescriptor {
    String name;
    String version;
    String hostname;
    int port;
}
