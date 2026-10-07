package org.kiwiproject.dynamodb.leader.dropwizard.health;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;
import static org.kiwiproject.metrics.health.HealthCheckResults.newHealthyResultBuilder;
import static org.kiwiproject.metrics.health.HealthCheckResults.newUnhealthyResultBuilder;
import static org.kiwiproject.metrics.health.HealthStatus.CRITICAL;

import com.codahale.metrics.health.HealthCheck;
import org.kiwiproject.dynamodb.leader.LeaderInfo;
import org.kiwiproject.dynamodb.leader.LeadershipStatus;
import org.kiwiproject.dynamodb.leader.dropwizard.ManagedLeaderLatch;

/**
 * Health check for a {@link ManagedLeaderLatch}. The check is unhealthy (CRITICAL) when:
 * <ul>
 *     <li>the latch is not started, or has been closed</li>
 *     <li>the leader cannot be determined because the lock record could not be read from DynamoDB</li>
 *     <li>there is no leader</li>
 *     <li>this participant believes it is the leader, but DynamoDB records a different participant as the leader</li>
 * </ul>
 * The last case is the closest equivalent of detecting more than one leader. A single participant cannot see the
 * other participants' views, so a monitor that checks every instance of a service should also look for more than
 * one instance reporting that it is the leader.
 * <p>
 * Each check reads the lock record from DynamoDB (one GetItem request).
 * <p>
 * The health check results contain the following details:
 * <table>
 *     <caption>Health Check Details</caption>
 *     <tr>
 *         <th>Name</th>
 *         <th>Description</th>
 *     </tr>
 *     <tr>
 *         <td>leader</td>
 *         <td>A boolean that is true if this participant is currently the leader, and false otherwise.</td>
 *     </tr>
 *     <tr>
 *         <td>leaderParticipant</td>
 *         <td>
 *             The ID of the participant that DynamoDB records as the leader.
 *             This will be null if the latch is not started or if no leader could be determined.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>thisParticipant</td>
 *         <td>
 *             The ID of this participant.
 *         </td>
 *     </tr>
 * </table>
 */
public class ManagedLeaderLatchHealthCheck extends HealthCheck {

    private static final String LEADER_DETAIL_NAME = "leader";
    private static final String LEADER_PARTICIPANT_DETAIL_NAME = "leaderParticipant";
    private static final String THIS_PARTICIPANT_DETAIL_NAME = "thisParticipant";

    private final ManagedLeaderLatch leaderLatch;

    /**
     * New health check instance for the given leader latch.
     *
     * @param leaderLatch the {@link ManagedLeaderLatch} to check
     */
    public ManagedLeaderLatchHealthCheck(ManagedLeaderLatch leaderLatch) {
        this.leaderLatch = requireNotNull(leaderLatch, "leaderLatch must not be null");
    }

    @Override
    protected Result check() {
        var thisParticipantId = leaderLatch.getId();
        var leadershipKey = leaderLatch.getLeadershipKey();
        var status = leaderLatch.checkLeadershipStatus();

        if (status instanceof LeadershipStatus.NotStarted || status instanceof LeadershipStatus.Closed) {
            return newUnhealthyResultBuilder(CRITICAL)
                    .withMessage("Leader latch for key %s is not started (status: %s)",
                            leadershipKey, status.getClass().getSimpleName())
                    .withDetail(LEADER_DETAIL_NAME, false)
                    .withDetail(LEADER_PARTICIPANT_DETAIL_NAME, null)
                    .withDetail(THIS_PARTICIPANT_DETAIL_NAME, thisParticipantId)
                    .build();
        }

        var isLeader = status.isLeader();
        var leaderInfo = leaderLatch.getLeader();

        if (leaderInfo instanceof LeaderInfo.LookupFailed failed) {
            return newUnhealthyResultBuilder(CRITICAL)
                    .withMessage("Unable to determine the leader for key %s: %s",
                            leadershipKey, failed.cause().toString())
                    .withDetail(LEADER_DETAIL_NAME, isLeader)
                    .withDetail(LEADER_PARTICIPANT_DETAIL_NAME, null)
                    .withDetail(THIS_PARTICIPANT_DETAIL_NAME, thisParticipantId)
                    .build();
        }

        if (leaderInfo instanceof LeaderInfo.NoLeader) {
            return newUnhealthyResultBuilder(CRITICAL)
                    .withMessage("There is NO leader for key %s", leadershipKey)
                    .withDetail(LEADER_DETAIL_NAME, isLeader)
                    .withDetail(LEADER_PARTICIPANT_DETAIL_NAME, null)
                    .withDetail(THIS_PARTICIPANT_DETAIL_NAME, thisParticipantId)
                    .build();
        }

        var leaderParticipantId = ((LeaderInfo.Leader) leaderInfo).participantId();

        if (isLeader && !leaderParticipantId.equals(thisParticipantId)) {
            return newUnhealthyResultBuilder(CRITICAL)
                    .withMessage("This participant (%s) believes it is the leader for key %s, but DynamoDB records %s as the leader",
                            thisParticipantId, leadershipKey, leaderParticipantId)
                    .withDetail(LEADER_DETAIL_NAME, true)
                    .withDetail(LEADER_PARTICIPANT_DETAIL_NAME, leaderParticipantId)
                    .withDetail(THIS_PARTICIPANT_DETAIL_NAME, thisParticipantId)
                    .build();
        }

        return newHealthyResultBuilder()
                .withMessage("Leader latch for key %s is started (has leadership? %s)", leadershipKey, isLeader)
                .withDetail(LEADER_DETAIL_NAME, isLeader)
                .withDetail(LEADER_PARTICIPANT_DETAIL_NAME, leaderParticipantId)
                .withDetail(THIS_PARTICIPANT_DETAIL_NAME, thisParticipantId)
                .build();
    }
}
