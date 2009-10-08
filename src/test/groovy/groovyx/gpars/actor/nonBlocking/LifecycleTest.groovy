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

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.ActorGroup
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.PooledActorGroup
import groovyx.gpars.actor.impl.AbstractPooledActor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * @author Vaclav Pech
 * Date: Feb 19, 2009
 */
public class LifecycleTest extends GroovyTestCase {

  ActorGroup group

  protected void setUp() {
    group = new PooledActorGroup(5)
  }

  protected void tearDown() {
    group.shutdown()
  }


  public void testDefaultStop() {
    final def barrier = new CyclicBarrier(2)
    final AtomicInteger counter = new AtomicInteger(0)

    final AbstractPooledActor actor = group.actor {
      barrier.await()
      counter.incrementAndGet()
      barrier.await()
    }.start()

    barrier.await()
    assert actor.isActive()
    barrier.await()
    assertEquals 1, counter.intValue()
    Thread.sleep 500
    assertFalse actor.isActive()
  }

  public void testDefaultStopAfterReact() {
    final def barrier = new CyclicBarrier(2)
    final AtomicInteger counter = new AtomicInteger(0)
    AtomicReference messagesReference = new AtomicReference(null)

    final AbstractPooledActor actor = group.actor {
      react {
        barrier.await()
        counter.incrementAndGet()
        barrier.await()
      }
    }.start()

    actor.metaClass {
      afterStop = {List messages ->
        messagesReference.set(messages)
        barrier.await()
      }
    }

    actor.send 'message'
    barrier.await()
    assert actor.isActive()
    barrier.await()
    assertEquals 1, counter.intValue()
    barrier.await()
    assertFalse actor.isActive()
    assertNotNull messagesReference.get()
  }

  public void testStop() {
    final AtomicInteger counter = new AtomicInteger(0)

    final AbstractPooledActor actor = group.actor {
      Thread.sleep 10000
      react {
        counter.incrementAndGet()
      }
    }.start()

    actor.metaClass {
      onInterrupt = {}
    }

    actor.send('message')
    actor.stop()

    Thread.sleep 1000
    assertEquals 0, counter.intValue()

    shouldFail(IllegalStateException) {
      actor.send 'message'
    }
  }

  public void testReentrantStop() {
    final def barrier = new CyclicBarrier(2)
    final def latch = new CountDownLatch(1)
    final AtomicInteger counter = new AtomicInteger(0)

    final AbstractPooledActor actor = group.actor {
      barrier.await()
      react {
      }
    }.start()

    actor.metaClass {
      afterStop = {List messages ->
        counter.incrementAndGet()
        latch.countDown()
      }
      onInterrupt = {}
    }

    actor.send 'message'
    actor.stop()
    actor.stop()
    actor.stop()
    latch.await()
    assertEquals 1, counter.intValue()
    assertFalse actor.isActive()
  }

  public void testStopWithoutMessageSent() {
    final def barrier = new CyclicBarrier(2)
    final def latch = new CountDownLatch(1)
    final AtomicInteger counter = new AtomicInteger(0)
    AtomicReference messagesReference = new AtomicReference(null)

    final AbstractPooledActor actor = group.actor {
      counter.incrementAndGet()
      barrier.await()

      react {
        counter.incrementAndGet()
      }
    }.start()

    actor.metaClass {
      afterStop = {List messages ->
        messagesReference.set(messages)
        latch.countDown()
      }
    }

    barrier.await()
    assert actor.isActive()
    assertEquals 1, counter.intValue()
    Thread.sleep 500

    actor.stop()

    latch.await(30, TimeUnit.SECONDS)
    assertFalse actor.isActive()
    assertEquals 1, counter.intValue()
    assertNotNull messagesReference.get()
  }

  public void testStopWithInterruption() {
    final def barrier = new CyclicBarrier(2)
    final def latch = new CountDownLatch(1)
    final AtomicInteger counter = new AtomicInteger(0)
    AtomicReference<List> messagesReference = new AtomicReference<List>(null)

    final AbstractPooledActor actor = group.actor {
      react {
        barrier.await()
        Thread.sleep(10000)
        counter.incrementAndGet()  //never reached
      }
    }.start()

    actor.metaClass {
      afterStop = {List messages ->
        messagesReference.set(messages)
        latch.countDown()
      }
      onInterrupt = {}
    }

    actor.send 'message1'
    actor.send 'message2'
    actor.send 'message3'
    barrier.await()
    assert actor.isActive()
    Thread.sleep 500
    actor.stop()

    latch.await(30, TimeUnit.SECONDS)
    assertEquals 0, counter.intValue()
    assertFalse actor.isActive()
    assertNotNull messagesReference.get()
    assertEquals 2, messagesReference.get().size()
  }

  public void testAfterStart() {
    final def afterStartBarrier = new CyclicBarrier(2)
    final AtomicBoolean afterStartFlag = new AtomicBoolean(false)

    final AbstractPooledActor actor = group.actor { }

    actor.metaClass {
      afterStart = { afterStartFlag.set true; afterStartBarrier.await() }
    }

    actor.start()

    afterStartBarrier.await(30, TimeUnit.SECONDS)
    assert afterStartFlag.get()
  }

  public void testOnInterrupt() {
    final def barrier = new CyclicBarrier(2)
    final def afterStopBarrier = new CyclicBarrier(2)
    final AtomicBoolean onInterruptFlag = new AtomicBoolean(false)
    AtomicReference<List> messagesReference = new AtomicReference<List>(null)

    final AbstractPooledActor actor = group.actor {
      react {
        barrier.await()
        Thread.sleep(10000)
      }
    }.start()

    actor.metaClass {
      afterStop = {List messages ->
        messagesReference.set(messages)
        afterStopBarrier.await()
      }
      onInterrupt = { onInterruptFlag.set true }
    }

    actor.send 'message1'
    actor.send 'message2'
    actor.send 'message3'
    barrier.await()
    actor.stop()

    afterStopBarrier.await(30, TimeUnit.SECONDS)
    assert onInterruptFlag.get()
    assertFalse actor.isActive()
    assertNotNull messagesReference.get()
    assertEquals 2, messagesReference.get().size()
  }

  public void testOnException() {
    final def barrier = new CyclicBarrier(2)
    final def afterStopBarrier = new CyclicBarrier(2)
    final AtomicBoolean onExceptionFlag = new AtomicBoolean(false)
    AtomicReference<List> messagesReference = new AtomicReference<List>(null)

    final AbstractPooledActor actor = group.actor {
      react {
        barrier.await()
        throw new RuntimeException('test')
      }
    }.start()

    actor.metaClass {
      afterStop = {List messages ->
        messagesReference.set(messages)
        afterStopBarrier.await()
      }
      onException = { onExceptionFlag.set true }
    }

    actor.send 'message1'
    actor.send 'message2'
    actor.send 'message3'
    barrier.await()

    afterStopBarrier.await(30, TimeUnit.SECONDS)
    assert onExceptionFlag.get()
    assertFalse actor.isActive()
    assertNotNull messagesReference.get()
    assertEquals 2, messagesReference.get().size()
  }

  public void testRestart() {
    final def barrier = new CyclicBarrier(2)
    final AtomicInteger counter = new AtomicInteger(0)

    final AbstractPooledActor actor = group.actor {
      barrier.await()
      counter.incrementAndGet()
      barrier.await()
    }.start()

    barrier.await()
    assert actor.isActive()
    barrier.await()
    assertEquals 1, counter.intValue()
    Thread.sleep 500
    assertFalse actor.isActive()

    actor.start()

    barrier.await()
    assert actor.isActive()
    barrier.await()
    assertEquals 2, counter.intValue()
    Thread.sleep 500
    assertFalse actor.isActive()
  }

  public void testDoubleStart() {
    final def barrier = new CyclicBarrier(2)
    final AtomicInteger counter = new AtomicInteger(0)

    final AbstractPooledActor actor = group.actor {
      barrier.await()
    }.start()

    shouldFail(IllegalStateException) {
      actor.start()
    }

    assert actor.isActive()
    barrier.await()
    Thread.sleep 500
    assertFalse actor.isActive()
  }

}
