//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.samples.actors

import org.gparallelizer.actors.impl.AbstractPooledActor
import org.gparallelizer.actors.PooledActorGroup
import static java.util.concurrent.TimeUnit.SECONDS

/**
 * Demonstrates the ability to receive multiple messages and selectively reply to some of them.
 * Notice the ability to sent HashMaps as well as instances of the Offer class as messages.
 * A custom pooled actor group is used to group the actors with a single thread pool.
 * The actors, which have submitted their offers, terminate, if they don't hear back from the actor within a timeout.
 * The main threads joins all actors to wait for their termination, since we're using a non-deamon actor group.
 * @author Vaclav Pech
 */

final PooledActorGroup group = new PooledActorGroup(1)

final AbstractPooledActor actor = group.actor {
    react {offerA, offerB, offerC ->
        reply 'Received your kind offer. Now processing it and comparing with others.'  //sent to all senders
        def winnerOffer = [offerA, offerB, offerC].min {it.price}
        winnerOffer.reply 'I accept your reasonable offer'  //sent to the winner only
        ([offerA, offerB, offerC] - [winnerOffer])*.reply 'Maybe next time'  //sent to the losers only
    }
}
actor.start()

final def a1 = group.actor {
    actor << new Offer(price: 10)
    loop {
        react(3, SECONDS) {
            println "Agent 1: $it"
        }
    }
}
a1.start()

final def a2 = group.actor {
    actor << [price:20]
    loop {
        react(3, SECONDS) {
            println "Agent 2: $it"
        }
    }
}.start()

final def a3 = group.actor {
    actor << new Offer(price : 5)
    loop {
        react(3, SECONDS) {
            println "Agent 3: $it"
        }
    }
}.start()

[actor, a1, a2, a3]*.join()

class Offer {
    int price
}


