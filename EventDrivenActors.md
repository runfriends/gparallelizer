# Use of event-driven actors #

_Event-driven actors_ share a **pool** of threads, which are dynamically assigned to actors when the actors need to **react** to messages sent to them. The threads are returned back to the pool once a message has been processed and the actor is idle waiting for some more messages to arrive. Actors become detached from the underlying threads and so a relatively small thread pool can serve potentially unlimited number of actors. Virtually unlimited scalability in number of actors is the main advantage of _event-based actors_ over _thread-bound ones_, where each actor has its own exclusive background thread associated with it.

Here are some examples of how to use _event-driven_ actors. This is how you create an actor that prints out all messages that it receives.

```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

def console = actor {
    loop {
        react {
            println it
        }
    }
```

Notice the _loop()_ method call, which ensures that the actor doesn't stop after having processed the first message.

As an alternative you can extend the AbstractPooledActor class and override the _act()_ method.

```
class CustomActor extends AbstractPooledActor {
    @Override protected void act() {
        loop {
            react {
                println it
            }
        }
    }
}

def console=new CustomActor()
```

Once you have the actor, you need to start it so that it attaches itself to the thread pool and can start accepting messages.

```
console.start()
console.send('Message')
```

The leftShift (<<) operator can be used to send messages to actors as a replacement for the _send()_ method.

```
console << 'Message'
```

### Creating an asynchronous service ###

```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

final def decryptor = actor {
    loop {
        react {String message->
            reply message.reverse()
        }
    }
}.start()

def console = actor {
    decryptor.send 'suonorhcnysa si yvoorG'
    react {
        println 'Decrypted message: ' + it
    }
}.start()

console.join()
```
As you can see, you create new actors with the _actor()_ method passing in the actor's body as a closure parameter. Inside the actor's body you can use _loop()_ to iterate, _react()_ to receive messages and _reply()_ to send a message to the actor, which has sent the currently processed message. With the _start()_ method you schedule the actor to the underlying thread pool for processing. When the decryptor actor doesn't find a message in its message queue, the _react()_ method gives up the thread and returns it back to the thread pool for other actors to pick up. Only after a new message arrives to the actor's message queue, the closure of the _react()_ method gets scheduled for processing with the pool. Event-based actors internally simulate continuations - actor's work is split into sequentially run chunks, which get invoked once a message is available in the inbox. Each chunk for a single actor can be performed by different thread from the thread pool.

Groovy's flexible syntax with closures allows our library to offer multiple ways to define actors. For instance, here's an example of an actor that waits for up to 30 seconds to receive a message, prints it out and terminates. Actors allow time DSL defined by org.codehaus.groovy.runtime.TimeCategory class to be used for timeout specification to the _react()_ method.

```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

def me = actor {
    friend.send('Hi')
    react(10.seconds) {
        //continue conversation
    }
}.start()

me.metaClass.onTimeout = {->friend.send('I see, busy as usual. Never mind.')}
me.join()
```
Notice the possibility to use Groovy meta-programming to define actor's lifecycle notification methods (e.g. _onTimeout()_) dynamically.

### Simple calculator ###

A little bit more realistic example of an event-driven actor that receives two numeric messages, sums them up and sends the result to the console actor.
```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

//not necessary, just showing that a single-threaded pool can still handle multiple actors
defaultPooledActorGroup.resize 1

final def console = actor {
    loop {
        react {
            println 'Result: ' + it
        }
    }
}.start()

final def calculator = actor {
    react {a ->
        react {b ->
            console.send(a + b)
        }
    }
}.start()

calculator.send 2
calculator.send 3

calculator.join()
```

You can group reception of multiple messages in a single _react()_ call.

```
final def calculator = actor {
    react {a, b ->
        console.send(a + b)
    }
}.start()
```
Notice that event-driven actors require special care regarding the _react()_ method. Since _event\_driven actors_ need to split the code into independent chunks assignable to different threads sequentially and **continuations** are not natively supported on JVM, the chunks are created artificially with tasks and exceptions. As a result the _react()_ and _loop()_ methods never return normally and actors' code must be structured accordingly. Again, this is in line with what Scala actors do.

### Concurrent Merge Sort Example ###

For comparison I'm also including a more involved example performing a concurrent merge sort of a list of integers using actors. You can see that thanks to flexibility of Groovy we came pretty close to the Scala's model, although I still miss Scala's pattern matching for message handling.

```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

Closure createMessageHandler(def parentActor) {
    return {
        react {List<Integer> message ->
            assert message != null
            switch (message.size()) {
                case 0..1:
                    parentActor.send(message)
                    break
                case 2:
                    if (message[0] <= message[1]) parentActor.send(message)
                    else parentActor.send(message[-1..0])
                    break
                default:
                    def splitList = split(message)

                    def child1 = actor(createMessageHandler(delegate))
                    def child2 = actor(createMessageHandler(delegate))
                    child1.start().send(splitList[0])
                    child2.start().send(splitList[1])

                    react {message1, message2 ->
                        parentActor.send merge(message1, message2)
                    }
            }
        }
    }
}

def console = new PooledActorGroup(1).actor {
    react {
        println "Sorted array:\t${it}"
        System.exit 0 
    }
}.start()

def sorter = actor(createMessageHandler(console))
sorter.start().send([1, 5, 2, 4, 3, 8, 6, 7, 3, 9, 5, 3])
console.join()
```

Since _event-driven actors_ reuse threads from a pool, the script will work with virtually **any size of a thread pool**, no matter how many actors are created along the way.

For brevity I didn't include the two helper methods split() and merge() in the code snippet. You can find them below.
```
def split(List<Integer> list) {
    int listSize = list.size()
    int middleIndex = listSize / 2
    def list1 = list[0..<middleIndex]
    def list2 = list[middleIndex..listSize - 1]
    return [list1, list2]
}

List<Integer> merge(List<Integer> a, List<Integer> b) {
    int i = 0, j = 0
    final int newSize = a.size() + b.size()
    List<Integer> result = new ArrayList<Integer>(newSize)

    while ((i < a.size()) && (j < b.size())) {
        if (a[i] <= b[j]) result << a[i++]
        else result << b[j++]
    }

    if (i < a.size()) result.addAll(a[i..-1])
    else result.addAll(b[j..-1])
    return result
}
```

### Actor lifecycle methods ###
Each Actor can define lifecycle observing methods, which will be called whenever a certain lifecycle event occurs.
  * afterStart() - called immediately after the Actor has been started, before the act() method is called the first time.
  * afterStop(List undeliveredMessages) - called right after the actor is stopped, passing in all the unprocessed messages from the queue.
  * onInterrupt(InterruptedException e) - called when the actor's thread gets interrupted. Thread interruption will result in the stopping the actor in any case.
  * onTimeout() - called when no messages are sent to the actor within the timeout specified for the currently blocking react method. Timeout will result in stopping the actor.
  * onException(Throwable e) - called when an exception occurs in the actor's event handler. Actor will stop after return from this method.

You can either define the methods statically in your Actor class or add them dynamically to the actor's metaclass:
```
def myActor = actor {...}

myActor.metaClass.onException = {
    log.error('Exception occurred', it)
}
```

### Pool management ###

Note: Describing pools and groups as they work in version 0.8 and later

_Event-driven_ actors can be organized into groups and as a default there's always an application-wide pooled actor group available. And just like the _PooledActors_ abstract factory can be used to create _event-driven_ actors in the default group, custom groups can be used as abstract factories to create new _event-driven_ actors instances belonging to these groups.

```
def myGroup = new PooledActorGroup()

def actor1 = myGroup.actor {
...
}

def actor2 = myGroup.actor {
...
}
```

The _event-driven_ actors belonging to the same group share the **underlying thread pool** of that group. The pool by default contains **n + 1 threads**, where **n** stands for the number of **CPUs** detected by the JVM. The **pool size** can be set **explicitly** either by setting the _gparallelizer.poolsize_ system property or individually for each actor group by specifying the appropriate constructor parameter.

```
def myGroup = new PooledActorGroup(10)  //the pool will contain 10 threads
```

The thread pool can be manipulated through the appropriate _PooledActorGroup_ class, which **delegates** to the _Pool_ interface of the thread pool. For example, the _resize()_ method allows you to change the pool size any time and the _resetDefaultSize()_ sets it back to the default value. The _shutdown()_ method can be called when you need to safely finish all tasks, destroy the pool and stop all the threads in order to exit JVM in an organized manner.

```
... (n+1 threads in the default pool after startup)

PooledActors.defaultPooledActorGroup.resize 1  //use one-thread pool

... (1 thread in the pool)

PooledActors.defaultPooledActorGroup.resetDefaultSize()

... (n+1 threads in the pool)

PooledActors.defaultPooledActorGroup.shutdown()
```

As an alternative to the _PooledActorGroup_, which creates a pool of daemon threads, the _NonDaemonPooledActorGroup_ class can be used when non-daemon threads are required.

```
def daemonGroup = new PooledActorGroup()

def actor1 = daemonGroup.actor {
...
}

def nonDaemonGroup = new NonDaemonPooledActorGroup()

def actor2 = nonDaemonGroup.actor {
...
}

class MyActor {
    def MyActor() {
        this.actorGroup = nonDaemonGroup
    }

    void act() {...}
}
```

_Event-driven_ actors belonging to the same group share the **underlying thread pool**. With pooled actor groups you can split your actors to leverage multiple thread pools of different sizes and so assign resources to different components of your system and tune their performance.

```
def coreActors = new NonDaemonPooledActorGroup(5)  //5 non-daemon threads pool
def helperActors = new PooledActorGroup(1)  //1 daemon thread pool

def priceCalculator = coreActors.actor {
...
}

def paymentProcessor = coreActors.actor {
...
}

def emailNotifier = helperActors.actor {
...
}

def cleanupActor = helperActors.actor {
...
}

//increase size of the core actor group
coreActors.resize 6 

//shutdown the group's pool once you no longer need the group to release resources
helperActors.shutdown()
```

Do not forget to shutdown custom pooled actor groups, once you no longer need them and their actors, to preserve system resources.

### Common trap: App terminates while actors do not receive messages ###

Most likely you're using daemon threads and pools, which is the default setting, and your main thread finishes. Calling _actor.join()_ on any, some or all of your actors would block the main thread until the actor terminates and thus keep all your actors running.
Alternatively use instances of _NonDaemonThreadActorGroup_ or _NonDaemonPooledActorGroup_ and assign some of your actors to these groups.
```
def nonDaemonGroup = new NonDaemonPooledActorGroup()
def myActor = nonDaemonGroup.actor {...}
```

alternatively
```
def nonDaemonGroup = new NonDaemonPooledActorGroup()

class MyActor extends AbstractPooledActor {
    def MyActor() {
        this.actorGroup = nonDaemonGroup
    }

    void act() {...}
}

def myActor = new MyActor()
```

## Special-purpose pooled actors ##
### Dynamic Dispatch Actor ###
Note: Available since version 0.8

The _DynamicDispatchActor_ class is a pooled actor allowing for an alternative structure of the message handling code. In general _DynamicDispatchActor_ repeatedly scans for messages and dispatches arrived messages to one of the _onMessage(message)_ methods defined on the actor. The _DynamicDispatchActor_ leverages the Groovy dynamic method dispatch mechanism under the covers.

```
import org.gparallelizer.actors.pooledActors.DynamicDispatchActor

final class MyActor extends DynamicDispatchActor {

    void onMessage(String message) {
        println 'Received string'
    }

    void onMessage(Integer message) {
        println 'Received integer'
    }

    void onMessage(Object message) {
        println 'Received object'
    }

    void onMessage(List message) {
        println 'Received list'
        stop()
    }
}

final def actor = new MyActor().start()

actor  << 1
actor  << ''
actor  << 1.0
actor  << new ArrayList()

actor.join()
```

In some scenarios, typically when no message-dependent state needs to be preserved on the actor, the dynamic dispatch code structure may be more intuitive than the one using nested _loop_ and _react_ statements.

### Reactive Actor ###
Note: Available since version 0.8

The _ReactiveActor_ class, constructed typically by calling _PooledActors.reactor()_ or _PooledActorGroup.reactor()_, allow for more event-driven like approach. When a reactive actor receives a message, the supplied block of code, which makes up the reactive actor's body, is run with the message as a parameter. The result returned from the code is sent in reply.

```
import org.gparallelizer.actors.pooledActors.PooledActorGroup

final def group = new PooledActorGroup()

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

for(i in (1..10)) {
    println "Double of $i = ${doubler.sendAndWait(i)}"
}

doubler.stop()
doubler.join()
```

Here's an example of an actor, which submits a batch of numbers to a _ReactiveActor_ for processing and then prints the results gradually as they arrive.

```
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.PooledActors

final def doubler = PooledActors.reactor {
    2 * it
}.start()

Actor actor = PooledActors.actor {
    (1..10).each {doubler << it}
    int i = 0
    loop {
        i += 1
        if (i > 10) stop()
        else {
            react {message ->
                println "Double of $i = $message"
            }
        }
    }
}.start()

actor.join()
doubler.stop()
doubler.join()
```

Essentially reactive actors provide a convenience shortcut for an actor that would wait for messages in a loop, process them and send back the result. This is schematically how the reactive actor looks inside:

```
public class ReactiveActor extends AbstractPooledActor {
    Closure body

    void act() {
        loop {
            react {message ->
                reply body(message)
            }
        }
    }
}
```
## Tips and tricks ##
### Structuring actor's code ###
When extending the _AbstractPooledActor_ class, you can call any actor's methods from within the _act()_ method and use the _react()_ or _loop()_ methods in them.
```
class MyActor extends AbstractPooledActor {

    protected void act() {
        handleA()
    }

    private void handleA() {
        react {a ->
            handleB(a)
        }
    }

    private void handleB(int a) {
        react {b ->
            println a + b
            reply a + b
        }
    }
}
```

Bear in mind that the methods _handleA()_ and _handleB()_ in all our examples never return, since they call _react()_, which itself never returns.

Alternatively, when using the _actor()_ factory method, you can add event-handling code through the meta class as closures.
```
Actor actor2 = actor {
    handleA()
}

actor2.metaClass {
    handleA = {->
        react {a ->
            handleB(a)
        }
    }

    handleB = {a ->
        react {b ->
            println a + b
            reply a + b
        }
    }
}
```

Closures, which have the actor set as their delegate can also be used to structure event-handling code.

```
Closure handleB = {a ->
    react {b ->
        println a + b
        reply a + b
    }
}

Closure handleA = {->
    react {a ->
        handleB(a)
    }
}

Actor actor3 = actor {
    handleA()
}
handleA.delegate = actor3
handleB.delegate = actor3
```
### Event-driven loops ###
When coding event-driven actors you have to have in mind that calls to _react()_ and _loop()_ methods never return. This becomes a bit of a challenge once you try to implement any types of loops in your actors.
On the other hand, if you leverage the fact that _react()_ never returns, you may call methods recursively without fear to fill up the stack. Look at the examples below, which respectively use the three described techniques for structuring actor's code.

A subclass of _AbstractPooledActor_
```
class MyLoopActor extends AbstractPooledActor {

    protected void act() {
        outerLoop()
    }

    private void outerLoop() {
        react {a ->
            println 'Outer: ' + a
            if (a!=0) innerLoop()
            else println 'Done'
        }
    }

    private void innerLoop() {
        react {b ->
            println 'Inner ' + b
            if (b == 0) outerLoop()
            else innerLoop()
        }
    }
}
```

Enhancing the actor's metaClass

```
Actor actor = actor {
    outerLoop()
}

actor.metaClass {
    outerLoop = {->
        react {a ->
            println 'Outer: ' + a
            if (a!=0) innerLoop()
            else println 'Done'
        }
    }

    innerLoop = {->
        react {b ->
            println 'Inner ' + b
            if (b==0) outerLoop()
            else innerLoop()
        }
    }
}
```

Using Groovy closures

```
Closure innerLoop

Closure outerLoop = {->
    react {a ->
        println 'Outer: ' + a
        if (a!=0) innerLoop()
        else println 'Done'
    }
}

innerLoop = {->
    react {b ->
        println 'Inner ' + b
        if (b==0) outerLoop()
        else innerLoop()
    }
}

Actor actor = actor {
    outerLoop()
}
outerLoop.delegate = actor
innerLoop.delegate = actor
```

Plus don't forget about the possibility to use the actor's _loop()_ method to create a loop that never stops before the actor terminates.

```
class MyLoopActor extends AbstractPooledActor {

    protected void act() {
        loop {
            outerLoop()
        }
    }

    private void outerLoop() {
        react {a ->
            println 'Outer: ' + a
            if (a!=0) innerLoop()
            else println 'Done for now, but will loop again'
        }
    }

    private void innerLoop() {
        react {b ->
            println 'Inner ' + b
            if (b == 0) outerLoop()
            else innerLoop()
        }
    }
}
```