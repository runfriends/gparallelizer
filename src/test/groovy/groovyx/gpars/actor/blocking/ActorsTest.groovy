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

package groovyx.gpars.actor.blocking

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors

/**
 *
 * @author Vaclav Pech
 * Date: Jan 9, 2009
 */
public class ActorsTest extends GroovyTestCase {
  public void testDefaultActor() {
    final AtomicInteger counter = new AtomicInteger(0)
    final CountDownLatch latch = new CountDownLatch(1)

    final Actor actor = Actors.actor {
      loop {
        final int value = counter.incrementAndGet()
        if (value == 3) {
          stop()
          latch.countDown()
        }
      }
    }
    actor.start()

    latch.await(30, TimeUnit.SECONDS)
    assertEquals 3, counter.intValue()
  }

  public void testDefaultactor() {
    final AtomicBoolean flag = new AtomicBoolean(false)
    final CountDownLatch latch = new CountDownLatch(1)

    final Actor actor = Actors.actor {
      flag.set(true)
      receive(10, TimeUnit.MILLISECONDS)
    }
    actor.metaClass.afterStop = {
      latch.countDown()
    }

    actor.start()

    latch.await(30, TimeUnit.SECONDS)
    assert flag.get()
  }

  public void testDefaultactorWithException() {
    final AtomicBoolean flag = new AtomicBoolean(false)
    final CountDownLatch latch = new CountDownLatch(1)

    final Actor actor = Actors.actor {
      throw new RuntimeException('test')
    }
    actor.metaClass.onException = {}
    actor.metaClass.afterStop = {
      flag.set(true)
      latch.countDown()
    }
    actor.start()

    latch.await(30, TimeUnit.SECONDS)
    assert flag.get()
  }
}