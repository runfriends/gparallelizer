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

import groovy.swing.SwingBuilder
import javax.swing.JFrame
import org.gparallelizer.actors.pooledActors.PooledActor
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.PooledActors

final def frame = new SwingBuilder().frame(title: 'Demo', defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
    vbox() {
        button('Click', actionPerformed: {
            bar()
            PooledActors.actor {
                react {
                    doLater {
                        println 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'
                    }
                }
            }.start().send 'Message'

        })
    }
}

frame.visible = true
frame.pack()

public void foo() {
    println 'Running'
    final def group = new PooledActorGroup(20)

    final def nestedActor = group.actor {
        println 'Started nested actor'
        react {
            println 'Finished nested actor'
            reply 20
        }
    }
    nestedActor.start()

    final PooledActor actor = group.actor {
        println 'Started an actor'
        nestedActor.sendAndWait(10)
        println 'Done'
    }
    actor.start()
    println 'Running controller'
}

public void bar() {
    final def group = new PooledActorGroup(20)

    final PooledActor actor = group.actor {
        println 'Started an actor ' + delegate + ":" + owner
        final def nestedActor = group.actor {
            println 'Started nested actor ' + delegate + ":" + owner
            react {
                println 'Finished nested actor ' + delegate + ":" + owner.owner + ":" + getResolveStrategy()
                reply 20
            }
        }.start()
        nestedActor.sendAndWait(10)
        println 'Done'
    }
    actor.start()
    println 'Running controller'

}
