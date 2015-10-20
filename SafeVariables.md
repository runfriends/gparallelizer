# Introduction #

In Clojure you can find a concept of Agents, which essentially behave like actors accepting code (functions) as messages. After reception the function is run against the internal state of the Agent and the return value of the function is considered to be the new internal state of the Agent.

Essentially, agents safe-guard mutable values by allowing only a single **agent-managed thread** to make modifications to them. The mutable values are **not directly accessible** from outside, but instead **requests have to be sent to the agent** and the agent guarantees to process the requests sequentially on behalf of the callers. Agents guarantee sequential execution of all requests and so consistency of the values.

Schematically:
```
agent = new Agent(0)  //created a new Agent wrapping an integer with initial value 0
agent.send {increment()}  //asynchronous send operation, sending the increment() function
...
//after some delay to process the message the internal Agent's state has been updated
...
assert agent.val== 1
```

To wrap integers, we can certainly use AtomicXXX types in Java/Groovy/Scala, but what if the state is a more complex object - list, map, shopping cart, etc.


# Concepts #

GParallelizer provides a SafeVariable class, which is a special-purpose thread-safe non-blocking reference implementation inspired by Agents in Clojure.

A SafeVariable wraps a reference to mutable state, held inside a single field, and accepts code (closures / commands) as messages, which can be sent to the SafeVariable just like to any other actor using the '<<' operator or any of the send() methods. After reception of a closure / command, the closure is invoked against the internal mutable field and can make changes to it. The closure is guaranteed to be run without intervention from other threads and so may freely alter the internal state of the SafeVariable held in the internal <i>data</i> field.

The whole update process is of the fire-and-forget type, since once the message (closure) is sent to the SafeVariable, the caller thread can go off to do other things and come back later to check the current value with SafeVariable.val or SafeVariable.valAsync(closure).

### Basic rules ###
  * The **return value** of the submitted closure is **sent in reply** to the sender of the closure.
  * If the message sent to a _SafeVariable_ is **not a closure**, it is considered to be a **new value** for the internal reference field.
  * The internal reference can also be changed using the **updateValue() method** from within the received closures.
  * The _val_ property of a _SafeVariable_ will safely return the current value of the SafeVariable.
  * The _valAsync()_ method will do the same **without blocking** the caller.
  * Since a _SafeVariable_ is an actor, you can call _stop()_ and _join()_ methods on it to shut it down explicitly.

# Examples #

### Shared list of members ###
The SafeVariable wraps a list of members, who have been added to the jug. To add a new member a message (command to add a member) has to be sent to the _jugMembers_ SafeVariable.
```
import org.gparallelizer.actors.pooledActors.SafeVariable

def jugMembers = new SafeVariable<List>(['Me'])  //add Me

jugMembers.send {it.add 'James'}  //add James

final Thread t1 = Thread.start {
    jugMembers.send {it.add 'Joe'}  //add Joe
}

final Thread t2 = Thread.start {
    jugMembers << {it.add 'Dave'}  //add Dave
    jugMembers << {it.add 'Alice'}  //add Alice
}

[t1, t2]*.join()
println jugMembers.val
jugMembers.valAsync {println "Current members: $it"}

System.in.read()
jugMembers.stop()
```

### Shared conference counting number of registrations ###
The Conference class allows registration and unregistration, however these methods can only be called from the commands sent to the _conference_ SafeVariable.
```
import org.gparallelizer.actors.pooledActors.SafeVariable

class Conference extends SafeVariable {
    def Conference() { super(0L) }
    private def register(long num) { data += num }
    private def unregister(long num) { data -= num }
}

final SafeVariable conference = new Conference()

final Thread t1 = Thread.start {
    conference << {register(10)}
}

final Thread t2 = Thread.start {
    conference << {register(5)}
}

final Thread t3 = Thread.start {
    conference << {unregister(3)}
}

[t1, t2, t3]*.join()

assert 12 == conference.val
```

## The printer service example ##

Another example - a not thread-safe printer service shared by multiple threads. The printer needs to have the document and quality properties set before printing, so obviously a potential for race conditions if not guarded properly. Callers don't want to block until the printer is available, which the fire-and-forget nature of actors solves very elegantly.
```
import org.gparallelizer.actors.pooledActors.SafeVariable

class PrinterService {
    String document
    String quality

    public void printDocument() {
        println "Printing $document in $quality quality"
        Thread.sleep 5000
        println "Done printing $document"
    }
}

def printer = new SafeVariable<PrinterService>(new PrinterService())

final Thread thread1 = Thread.start {
    for (num in (1..3)) {
        final String text = "document $num"
        printer << {printerService ->
            printerService.document = text
            printerService.quality = 'High'
            printerService.printDocument()
        }
        Thread.sleep 200
    }
    println 'Thread 1 is ready to do something else. All print tasks have been submitted'
}

final Thread thread2 = Thread.start {
    for (num in (1..4)) {
        final String text = "picture $num"
        printer << {printerService ->
            printerService.document = text
            printerService.quality = 'Medium'
            printerService.printDocument()
        }
        Thread.sleep 500
    }
    println 'Thread 2 is ready to do something else. All print tasks have been submitted'
}

[thread1, thread2]*.join()
printer << {stop()}
printer.join()
```