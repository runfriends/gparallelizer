package org.gparallelizer.actors.pooledActors

import static org.gparallelizer.actors.pooledActors.PooledActors.retrieveDefaultPool

/**
 *
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public class SingleThreadLoopTest extends LoopTest {

    protected void setUp() {
        super.setUp();
        retrieveDefaultPool().resize(1)
    }
}