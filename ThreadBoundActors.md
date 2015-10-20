# Use of thread-bound actors #

For example, this is how you create an actor that prints out all messages that it receives.

```
import static org.gparallelizer.actors.Actors.*

def console = actor {
    println receive()
}
```

Alternativelly you can extend one of the Actor's classes and override the act() method.

```
class CustomActor extends BoundThreadActor {
    @Override protected void act() {
        println receive()
    }
}

def console=new CustomActor()
```

Once you have the actor, you need to start it so that it creates its background thread and can accept messages.

```
console.start()
console.send('Message')
```

The leftShift (<<) operator can be used to send messages to actors as a replacement for the _send()_ method.

```
console << 'Message'
```

Groovy's flexible syntax with closures allows our library to offer multiple ways to define actors. For instance, here's an example of an actor that waits for up to 30 seconds to receive a message, prints it out and terminates. Something I call a one-shot actor.

```
def notifier = oneShotActor {
    println receive(30.seconds) ?: 'No message'
}
```

Notice the possible alternative way to retrieve the received value from the receive() method - the value is passed as a parameter into a closure.

```
def notifier = oneShotActor {
    receive(30.seconds) {
        println it?:'No message'
    }
}
```

### Simple calculator ###

A little bit more realistic example of an actor that receives two numeric messages, sums them up and sends the result to the console actor.
```
import static org.gparallelizer.actors.Actors.*

def console = oneShotActor {
    println 'Result: ' + receive()
}.start()

def calculator = oneShotActor {
    int a = receive()
    int b = receive()
    console.send(a + b)
}.start()

calculator.send(2)
calculator.send(3) 

calculator.join()
```

### Concurrent Merge Sort Example ###

For comparison I'm including a more involved example performing a concurrent merge sort of a list of integers using actors. You can see that thanks to flexibility of Groovy we came pretty close to the Scala's model, although I still miss Scala's pattern matching for message handling.

```
import static org.gparallelizer.actors.Actors.*

Closure createMessageHandler(def parentActor) {
    return {
        receive {List<Integer> message ->
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

                    def child1 = oneShotActor(createMessageHandler(delegate))
                    def child2 = oneShotActor(createMessageHandler(delegate))
                    child1.start().send(splitList[0])
                    child2.start().send(splitList[1])

                    parentActor.send merge(receive(), receive())
            }
        }
    }
}

def console = oneShotActor {
    println "Sorted array:\t${receive()}"
}.start()

def sorter = oneShotActor(createMessageHandler(console))
sorter.start().send([1, 5, 2, 4, 3, 8, 6, 7, 3, 4, 5, 2, 2, 9, 8, 7, 6, 7, 8, 1, 4, 1, 7, 5, 8, 2, 3, 9, 5, 7, 4, 3])
console.join()
```

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

### Actors creation ###
_GParallelizer_ provides multiple Actor classes, which differ in the type of the message queue. For example, the DefaultThreadActor uses unbounded queue, BoundedActor uses a bounded one and the SynchronousActor uses an SynchronousQueue, where send() and receive() operations must meet for both to proceed further.

Actors have their _act()_ method run periodically by the actors' thread until the _stop()_ method is called on the Actor. So called one-shot actors have the _stop()_ method called automatically after the _act()_ method finishes the first time. They are aimed at handling non-repeating tasks.

Developers may either extend one of the Actor classes and override the _act()_ method or use one of the helper methods in the _Actors_ class to have an Actor created around a supplied closure.

_Thread-bound_ actors can be organized into groups and as a default there's always an application-wide thread actor group available. And just like the _Actors_ abstract factory can be used to create actors in the default group, custom groups can be used as abstract factories to create new actors instances belonging to these groups.

Note: Describing groups as they work in version 0.8 and later

```
def myGroup = new ThreadActorGroup()

def actor1 = myGroup.actor {
...
}

def actor2 = myGroup.oneShotActor {
...
}
```

The _thread-bound_ actors from the same group share **the underlying thread pool** of that group. Although _thread-bound_ actors use **a single thread for their whole life-time**, the threads are not created for each actor individually, but they're taken from the group's thread pool instead. The pool resizes automatically and the initial size can be specified either by setting the _gparallelizer.poolsize_ system property or individually for each actor group by specifying the appropriate constructor parameter.
```
def myGroup = new ThreadActorGroup(10)  //the pool starts with 10 threads, but will resize if needed
```
The thread pool can be manipulated through the appropriate _ThreadActorGroup_ class, which **delegates** to the _Pool_ interface of the thread pool. For example, the _resize()_ method allows you to change the pool default size any time and the _resetDefaultSize()_ sets it back to the default value. The _shutdown()_ method can be called when you need to safely finish all tasks, destroy the pool and stop all the threads in order to exit JVM in an organized manner.

```
def myGroup = new ThreadActorGroup(10)
myGroup.resize 20
myGroup.resetDefaultSize()
myGroup.shutdown()
```

As an alternative to the _ThreadActorGroup_, which creates a pool of daemon threads, the _NonDaemonThreadActorGroup_ class can be used when non-daemon threads are required.

```
def daemonGroup = new ThreadActorGroup()

def actor1 = daemonGroup.actor {
...
}

def nonDaemonGroup = new NonDaemonThreadActorGroup()

def actor2 = nonDaemonGroup.actor {
...
}
```

Do not forget to shutdown custom thread actor groups, once you no longer need them and their actors, to preserve system resources.

### Common trap: App terminates while actors do not receive messages ###

Most likely you're using daemon threads and pools, which is the default setting, and your main thread finishes. Calling _actor.join()_ on any, some or all of your actors would block the main thread until the actor terminates and thus keep all your actors running.
Alternatively use instances of _NonDaemonThreadActorGroup_ or _NonDaemonPooledActorGroup_ and assign some of your actors to these groups.
```
def nonDaemonGroup = new NonDaemonThreadActorGroup()
def myActor = nonDaemonGroup.actor {...}
```

alternatively
```
def nonDaemonGroup = new NonDaemonThreadActorGroup()

class MyActor extends DefaultThreadActor {
    def MyActor() {
        this.actorGroup = nonDaemonGroup
    }

    void act() {...}
}

def myActor = new MyActor()
```

### Actor lifecycle methods ###
Each Actor can define lifecycle observing methods, which will be called by the Actor's background thread whenever a certain lifecycle event occurs.
  * afterStart() - called immediately after the Actor's background thread has been started, before the act() method is called the first time.
  * afterStop(List undeliveredMessages) - called right after the actor is stopped, passing in all the messages from the queue.
  * onInterrupt(InterruptedException e) - called when the actor's thread gets interrupted. Thread interruption will result in stopping the actor in any case.
  * onException(Throwable e) - called when an exception occurs in the actor's thread. Throwing an exception from this method will stop the actor.

You can either define the methods statically in your Actor class or add them dynamically to the actor's metaclass:
```
import static org.gparallelizer.actors.Actors.*

def myActor = actor {...}

myActor.metaClass.onException = {
    log.error('Exception occured', it)
    if (it instanceof Error) throw it
}
```

### TimeCategory DSL use ###
Actors allow time DSL defined by org.codehaus.groovy.runtime.TimeCategory class to be used for timeout specification to the receive() method.
```
import static org.gparallelizer.actors.Actors.*

def me = actor {
    friend.send('Hi')
    def reply = receive(10.seconds)
    if (!reply) friend.send('I said Hi! Are your there man?')
}
```

### Actors as Mixins ###
As of 0.5 release actors can be mixed into other classes.
```
        Company.metaClass {
            mixin DefaultThreadActor

            act = {->
                receive {
                    println it
                }
            }

            afterStop = {List undeliveredMessages ->
                ...
            }
        }

final Company company = new Company(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])
company.start()
company.send("Message")
...
company.stop()
```

Actors can also extend individual instances only.
```
final Company company = new Company(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])

company.metaClass {
    mixin DefaultThreadActor

    act = {->
        receive {
            println it
        }
    }
}

company.start()
company.send("Message")
...
company.stop()

```
## Tips and tricks ##
### Structuring actor's code ###
When extending the _AbstractThreadActor_ class, you can call any actor's methods from within the _act()_ method and use the _receive()_ method in them.
```
class MyActor extends DefaultThreadActor {

    protected void act() {
        handleA()
    }

    private void handleA() {
        receive {a ->
            handleB(a)
        }
    }

    private void handleB(int a) {
        receive {b ->
            println a + b
            reply a + b
        }
    }
}
```

Alternatively, when using the _actor()_ factory method, you can add actor code through the meta class as closures.
```
Actor actor2 = actor {
    handleA()
}

actor2.metaClass {
    handleA = {->
        receive {a ->
            handleB(a)
        }
    }

    handleB = {a ->
        receive {b ->
            println a + b
            reply a + b
        }
    }
}
```

Closures, which have the actor set as their delegate can also be used to structure event-handling code.

```
Closure handleB = {a ->
    receive {b ->
        println a + b
        reply a + b
    }
}

Closure handleA = {->
    receive {a ->
        handleB(a)
    }
}

Actor actor3 = actor {
    handleA()
}
handleA.delegate = actor3
handleB.delegate = actor3
```