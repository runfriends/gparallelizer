package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.pooledActors.PooledActors

/**
 *
 * @author Vaclav Pech
 * Date: Feb 20, 2009
 */
public class SingleThreadedMessagingTest extends MessagingTest {

    protected void setUp() {
        super.setUp();
        PooledActors.defaultPooledActorGroup.resize(1)
    }

    protected void tearDown() {
        super.tearDown();
        PooledActors.defaultPooledActorGroup.resize(5)
    }
}