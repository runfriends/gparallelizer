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

package groovyx.gpars.actor

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.codehaus.groovy.runtime.NullObject

/**
 * A pooled actor allowing for an alternative structure of the message handling code.
 * In general DynamicDispatchActor repeatedly scans for messages and dispatches arrived messages to one
 * of the onMessage(message) methods defined on the actor.
 * <pre>
 * final class MyActor extends DynamicDispatchActor {*      void onMessage(String message) {*          println 'Received string'
 *}*      void onMessage(Integer message) {*          println 'Received integer'
 *}*      void onMessage(Object message) {*          println 'Received object'
 *}*      void onMessage(NullObject nullMessage) {*          println 'Received null'
 *}*} </pre>
 *
 * Method when {...} provides an alternative way to define message handlers
 *
 * @author Vaclav Pech, Alex Tkachman, Dierk Koenig
 * Date: Jun 26, 2009
 */

public class DynamicDispatchActor extends AbstractPooledActor {

    /**
     * Creates a new instance without any when handlers registered
     */
    DynamicDispatchActor() {
        this(null)
    }

    /**
     * Creates an instance, processing all when{} calls in the supplied closure
     * @param closure A closure to run against te actor, typically to register handlers
     */
    DynamicDispatchActor(Closure closure) {
        if (closure) {
            Closure cloned = (Closure) closure.clone()
            cloned.resolveStrategy = Closure.DELEGATE_FIRST
            cloned.delegate = this
            cloned.call()
        }
    }

    /**
     * Loops reading messages using the react() method and dispatches to the corresponding onMessage() method.
     */
    final void act() {
        loop {
            react {msg ->
                if (msg == null)
                    msg = NullObject.nullObject
                onMessage msg
            }
        }
    }

    void when(Closure closure) {
        DefaultGroovyMethods.getMetaClass(this).onMessage closure
    }
}
