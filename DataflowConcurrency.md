# Introduction #

Check out the small example written in Groovy using GParallelizer, which sums results of calculations performed by three concurrently run threads:
```
import static org.gparallelizer.dataflow.DataFlow.start

final def x = new DataFlowVariable()
final def y = new DataFlowVariable()
final def z = new DataFlowVariable()

start {
    z << x.val + y.val
    println "Result: ${z.val}"
}

start {
    x << 10
}

start {
    y << 5
}
```

We start three logical threads, which run in parallel and perform their tasks. When a thread needs to read a value from _DataFlowVariable_ (through the val property), it will block until the value has been set by another thread (using the '<<' operator). Each _DataFlowVariable_ can be set only once in its lifetime. Notice that you don't have to bother with ordering and synchronizing the threads and their access to shared variables. The values are magically transferred among threads at the right time without your intervention.

**Implementation detail:** The three threads in the example **do not necessarily need to be three physical threads**. They're so-called "green" or "logical" threads and can be mapped under the covers to any number of physical threads.

# Benefits #
Here's what you gain by using Dataflow Concurrency (by [Jonas Bonér](http://jonasboner.com/)):

  * No race-conditions
  * No live-locks
  * Deterministic deadlocks
  * Completely deterministic programs
  * BEAUTIFUL code.

This doesn't sound bad, does it?

# Concepts #

### Dataflow programming ###
_Quoting Wikipedia_

Operations (in Dataflow programs) consist of "black boxes" with inputs and outputs, all of which are always explicitly defined. They run as soon as all of their inputs become valid, as opposed to when the program encounters them. Whereas a traditional program essentially consists of a series of statements saying "do this, now do this", a dataflow program is more like a series of workers on an assembly line, who will do their assigned task as soon as the materials arrive. This is why dataflow languages are inherently parallel; the operations have no hidden state to keep track of, and the operations are all "ready" at the same time.

### Principles ###
With Dataflow Concurrency you can safely share variables across threads. These variables (in Groovy instances of the _DataFlowVariable_ class) can only be assigned (using the '<<' operator) a value once in their lifetime. The values of the variables, on the other hand, can be read multiple times (in Groovy through the val property), even before the value has been assigned. In such cases the reading 'green' thread is suspended until the value is set by another 'green' thread.
So you can simply write your code sequentially using Dataflow Variables and the underlying mechanics will make sure you get all the values you need in a thread-safe manner.

Briefly, you generally perform three operations with Dataflow variables:
  * Create a dataflow variable
  * Wait for the variable to be bound (read it)
  * Bind the variable (write to it)

And these are the three essential rules your programs have to follow:
  * When the program encounters an unbound variable it waits for a value.
  * It is not possible to change the value of a dataflow variable once it is bound.
  * Dataflow variables makes it easy to create concurrent stream agents.

# Dataflow Streams #

Before I show you the final and most catchy demo, you should know a bit about streams to have a full picture of Dataflow Concurrency. Except for dataflow variables there's also a concept of _DataFlowStreams_ that you can leverage. You may think of them as thread-safe buffers or queues. Check out a typical producer-consumer demo:

```
import static org.gparallelizer.dataflow.DataFlow.start

def words = ['Groovy', 'fantastic', 'concurrency', 'fun', 'enjoy', 'safe', 'GParallelizer', 'data', 'flow']
final def buffer = new DataFlowStream()

start {
    for (word in words) {
        buffer << word.toUpperCase()  //add to the buffer
    }
}

start {
    while(true) println buffer.val  //read from the buffer in a loop
}
```

# A simple mashup example #
In the example we're downloading the front pages of three popular web sites, each in their own thread, while in a separate thread we're filtering out sites talking about Groovy today and forming the output. The output thread synchronizes automatically with the three download threads on the three Dataflow variables through which the content of each website is passed to the output thread.

```
import static org.gparallelizer.Asynchronizer.*
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start


/**
 * A simple mashup sample, downloads content of three websites
 * and checks how many of them refer to Groovy.
 */

def dzone = new DataFlowVariable()
def jroller = new DataFlowVariable()
def theserverside = new DataFlowVariable()

start {
    println 'Started downloading from DZone'
    dzone << 'http://www.dzone.com'.toURL().text
    println 'Done downloading from DZone'
}

start {
    println 'Started downloading from JRoller'
    jroller << 'http://www.jroller.com'.toURL().text
    println 'Done downloading from JRoller'
}

start {
    println 'Started downloading from TheServerSide'
    theserverside << 'http://www.theserverside.com'.toURL().text
    println 'Done downloading from TheServerSide'
}

start {
    doAsync {
        println "Number of Groovy sites today: " +
                ([dzone, jroller, theserverside].findAllAsync {
                    it.val.toUpperCase().contains 'GROOVY'
                }).size()
    }
    System.exit 0
}
```

## A mashup variant with methods ##
To avoid giving wrong impression about structuring the Dataflow code, here's a rewrite of the mashup example, with a _downloadPage()_ method performing the actual download in a separate thread and returning a DataFlowVariable instance, so that the main thread could eventually get hold of the downloaded content.
Dataflow variables can obviously be passed around as parameters or return values.
```
package org.gparallelizer.samples.dataflow

import static org.gparallelizer.Asynchronizer.*
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start


/**
 * A simple mashup sample, downloads content of three websites and checks how many of them refer to Groovy.
 */
final List urls = ['http://www.dzone.com', 'http://www.jroller.com', 'http://www.theserverside.com']

start {
    def pages = urls.collect { downloadPage(it) }
    doAsync {
        println "Number of Groovy sites today: " +
                (pages.findAllAsync {
                    it.val.toUpperCase().contains 'GROOVY'
                }).size()
    }
    System.exit 0
}

def downloadPage(def url) {
    def page = new DataFlowVariable()
    start {
        println "Started downloading from $url"
        page << url.toURL().text
        println "Done downloading from $url"
    }
    return page
}
```
# A physical calculation example #

Dataflow programs naturally scale with the number of processors. Up to a certain level, the more processors you have the faster the program runs.
Check out, for example, the following script, which calculates parameters of a simple physical experiment and prints out the results. Each "green" thread performs its part of the calculation, it may depend on values calculated by some other threads as well as its result might be needed by some other "green" thread. With Dataflow Concurrency you can split the work between "green" threads or reorder the threads themselves as you like and the dataflow mechanics will ensure the calculation will be accomplished correctly.

```
import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start

final def mass = new DataFlowVariable()
final def radius = new DataFlowVariable()
final def volume = new DataFlowVariable()
final def density = new DataFlowVariable()
final def acceleration = new DataFlowVariable()
final def time = new DataFlowVariable()
final def velocity = new DataFlowVariable()
final def decelerationForce = new DataFlowVariable()
final def deceleration = new DataFlowVariable()
final def distance = new DataFlowVariable()

start {
    println """
Calculating distance required to stop a moving ball.
====================================================
The ball has a radius of ${radius.val} meters and is made of a material with ${density.val} kg/m3 density,
which means that the ball has a volume of ${volume.val} m3 and a mass of ${mass.val} kg.
The ball has been accelerating with ${acceleration.val} m/s2 from 0 for ${time.val} seconds and so reached a velocity of ${velocity.val} m/s.

Given our ability to push the ball backwards with a force of ${decelerationForce.val} N (Newton), we can cause a deceleration
of ${deceleration.val} m/s2 and so stop the ball at a distance of ${distance.val} m.

=======================================================================================================================
This example has been calculated asynchronously in multiple threads using GParallelizer DataFlow concurrency in Groovy.
Author: ${author.val}
"""

    System.exit 0
}

start {
    mass << volume.val * density.val
}

start {
    volume << Math.PI * (radius.val ** 3)
}

start {
    radius << 2.5
    density << 	998.2071  //water
    acceleration << 9.80665 //free fall
    decelerationForce << 900
}

start {
    println 'Enter your name:'
    def name = new InputStreamReader(System.in).readLine()
    author << (name?.trim()?.size()>0 ? name : 'anonymous')
}

start {
    time << 10
    velocity << acceleration.val * time.val
}

start {
    deceleration << decelerationForce.val / mass.val
}

start {
    distance << deceleration.val * ((velocity.val/deceleration.val) ** 2) * 0.5
}
```

Note: I did my best to make all the physical calculations right. Feel free to change the values and see how long distance you need to stop the rolling ball.

# Deterministic deadlocks #
If you happen to introduce a deadlock in your dependencies, the deadlock will occur each time you run the code. No randomness allowed. That's one of the benefits of Dataflow concurrency.
```
start {
    println a.val
    b << 'Hi there'
}

start {
    println b.val
    a << 'Hello man'
}
```

# DataFlows map #

As a handy shortcut the _DataFlows_ class can help you reduce the amount of code you have to write to leverage Dataflow variables.

```
def df = new DataFlows()
df.x = 'value1'
assert df.x == 'value1'

DataFlow.start {df.y = 'value2}

assert df.y == 'value2'
```
Think of DataFlows as a map with Dataflow Variables as keys storing their bound values as appropriate map values. The semantics of reading a value (e.g. df.x) and binding a value (e.g. df.x = 'value') remain identical to the semantics of plain Dataflow Variables (x.val and x << 'value' respectively).

# Implementation in GParallelizer #

The Dataflow Concurrency in GParallelizer builds on top of its actor support. Each time you create a new "thread" with the _DataFlow.start()_ factory method, an actor is started to process the passed-in code parameter. All of the dataflow thread actors share a thread pool and so the number threads created through _DataFlow.start()_ factory method don't need to correspond to the number of physical threads required from the system.

### Combining actors and Dataflow Concurrency ###

The good news is that you can combine actors and Dataflow Concurrency in any way you feel fit for your particular problem at hands. Since the _DataFlow.start()_ method returns a subclass of _AbstractPooledActor_ (already started, however), you can set the lifecycle event handlers on it just like you can on the _AbstractPooledActor_ class. It's also perfectly valid to send messages to the "thread" and accept incoming messages in the body of the "thread" using _react()_ and _loop()_.

```
final DataFlowVariable a = new DataFlowVariable()

final AbstractPooledActor doubler = PooledActors.actor {
    react {
        a << 2 * it
    }
}.start()

final AbstractPooledActor thread = start {
    react {
        doubler.send it  //send a number to the doubler
        println "Result ${a.val}"  //wait for the result to be bound to 'a'
    }
}

thread << 10
```

In the example you see the "thread" using both messages and a _DataFlowVariable_ to communicate with the _doubler_ actor.

### Using plain java threads ###
The _DataFlowVariable_ as well as the _DataFlowStream_ classes can obviously be used from any thread of your application, not only the actor-backed _DataFlow.start()_ threads. Consider the following example:
```
import org.gparallelizer.dataflow.DataFlowVariable

final DataFlowVariable a = new DataFlowVariable<String>()
final DataFlowVariable b = new DataFlowVariable<String>()

Thread.start {
    println "Received: $a.val"
    Thread.sleep 2000
    b << 'Thank you'
}

Thread.start {
    Thread.sleep 2000
    a << 'An important message from the second thread'
    println "Reply: $b.val"
}
```
We're creating two plain _java.lang.Thread_ instances, which exchange data using the two data flow variables. Obviously, neither the actor lifecycle methods, nor the send/react functionality or thread pooling take effect in such scenarios.

# Further reading #

[Scala Dataflow library by Jonas Bonér](http://github.com/jboner/scala-dataflow/tree/f9a38992f5abed4df0b12f6a5293f703aa04dc33/src)

[JVM concurrency presentation slides by Jonas Bonér](http://jonasboner.com/talks/state_youre_doing_it_wrong/html/all.html)

[Dataflow Concurrency library for Ruby](http://github.com/larrytheliquid/dataflow/tree/master)