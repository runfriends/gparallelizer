package org.gparallelizer.actors

import org.gparallelizer.actors.pooledActors.Pool

/**
 * The common functionality for all actor groups.
 *
 * @author Vaclav Pech
 */
public abstract class AbstractActorGroup {

    /**
     * Stored the group actors' thread pool
     */
    protected @Delegate Pool threadPool

    /**
     * Indicates whether actors in this group use ForkJoin thread pool
     */
    final boolean forkJoinUsed

    def AbstractActorGroup() { this.forkJoinUsed = useFJPool() }

    def AbstractActorGroup(final boolean forkJoinUsed) { this.forkJoinUsed = forkJoinUsed }

    /**
     * Checks the gparallelizer.useFJPool system property and returns, whether or not to use a fork join pool
     */
    protected final boolean useFJPool() {
        return 'true' == System.getProperty("gparallelizer.useFJPool")?.trim()?.toLowerCase()
    }
}