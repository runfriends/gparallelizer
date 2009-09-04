package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * Shows cooperation between an actor and a dataflow thread.
 * Since dataflow threads are plain pooled actors, they can react to messages just like actors do.
 */

final DataFlowVariable a = new DataFlowVariable()

final AbstractPooledActor doubler = PooledActors.actor {
    react {
        a << 2 * it
    }
}.start()

final AbstractPooledActor thread = start {
    react {
        doubler << it  //send a number to the doubler
        println "Result ${a.val}"  //wait for the result to be bound to 'a'
    }
}

thread << 10

System.in.read()
System.exit 0 