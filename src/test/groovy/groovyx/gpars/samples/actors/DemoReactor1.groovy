//  GPars (formerly GParallelizer)
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

package groovyx.gpars.samples.actors

import groovyx.gpars.actor.PooledActorGroup

/**
 * Demonstrates use of reactor - a specialized actor responding to incomming messages with result of running its body
 * on the message.
 */

final def group = new PooledActorGroup(4)

final def doubler = group.reactor {
    2 * it
}.start()

group.actor {
    println 'Double of 10 = ' + doubler.sendAndWait(10)
}.start()

group.actor {
    println 'Double of 20 = ' + doubler.sendAndWait(20)
}.start()

group.actor {
    println 'Double of 30 = ' + doubler.sendAndWait(30)
}.start()

for (i in (1..10)) {
    println "Double of $i = ${doubler.sendAndWait(i)}"
}

doubler.stop()
doubler.join()