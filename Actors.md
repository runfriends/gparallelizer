_Actors_ in GParallelizer were inspired by Actors library in Scala. They allow for messaging-based concurrency model, built from independent active objects that exchange messages and have no mutable shared state. Actors can naturally avoid issues like deadlocks, livelocks or starvation, so typical for shared memory.
A nice wrap-up of the key [concepts behind actors](http://ruben.savanne.be/articles/concurrency-in-erlang-scala) was written recently by Ruben Vermeersch

You may also check out the [examples of actor use](ActorsExamples.md).

The actors library allows for interesting applications, like [SafeVariable (aka Clojure Agents)](http://code.google.com/p/gparallelizer/wiki/SafeVariables).

# Types of Actors #

**GParallelizer** comes with two types of actors, both of which have their uses. _Thread-bound actors_ maintain their own thread of execution, whereas _event-driven actors_ share a common thread pool, from which they borrow threads whenever they must react to an event - typically a message sent to them.

## Event-driven actors ##

Although _thread-bound actors_ can perform better in some scenarios and are probably a good default choice when it comes to long calculation tasks, they exhibit scalability limitations due to the natural limits for the number of concurrently run threads imposed by current HW platforms. _Event-driven actors_, which share a relatively small thread pool and avoid these threading limitations, on the other hand, are a bit trickier to use since they require computing tasks to be detachable from and re-attachable to the underlying threads. Actor's code is processed in chunks separated by quiet periods of waiting for new events (messages). This can be naturally modeled through _continuations_. As JVM doesn't support continuations directly, they have to be simulated in the actors frameworks, which has slight impact on organization of the actors' code. However, the benefits in most cases outweigh the difficulties.

```
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

class GameMaster extends AbstractPooledActor {
       int secretNum

       void afterStart()
       {
               secretNum = new Random().nextInt(10)
       }

       void act()
       {
               loop
               {
                       react { int num ->
                               if      ( num > secretNum )
                                       reply 'too large'
                               else if ( num < secretNum )
                                       reply 'too small'
                               else
                               {
                                       reply 'you win'
                                       stop()
                                       System.exit 0
                               }
                       }
               }
       }
}

class Player extends AbstractPooledActor {
       String              name
       AbstractPooledActor server
       int                 myNum

       void act()
       {
               loop
               {
                       myNum = new Random().nextInt(10)

                       server.send myNum

                       react {
                               switch( it )
                               {
                                       case 'too large': println "$name: $myNum was too large"; break
                                       case 'too small': println "$name: $myNum was too small"; break
                                       case 'you win':   println "$name: I won $myNum"; stop(); break
                               }
                       }
               }
       }
}

def master = new GameMaster().start()
new Player( name: 'Player', server: master ).start()

[master, player]*.join()
```
example by _Jordi Campos i Miralles, Departament de Matemàtica Aplicada i Anàlisi, MAiA Facultat de Matemàtiques, Universitat de Barcelona_

# Usage of Actors #

**GParallelizer** provides consistent API and DSL for both _thread-bound actors_ and _event-driven actors_. The differences in use are only marginal and relate to the different threading nature of these two types of actors.

## Sending messages ##

Messages can be sent to actors using the _send()_ method. Alternatively, the _<<_ operator can be used. A family of _sendAndWait()_ methods is available to block the caller until a reply from the actor is available. The reply is returned from the _sendAndWait()_ method as a return value.
The _sendAndWait()_ methods may also return after a timeout expires or in case of termination of the called actor.
```
actor << 'Message'
actor.send 'Message'
def reply1 = actor.sendAndWait('Message')
def reply2 = actor.sendAndWait(10, TimeUnit.SECONDS, 'Message')
def reply3 = actor.sendAndWait(10.seconds, 'Message')
```

All _send()_ and _sendAndWait()_ methods will throw an exception if invoked on a non-active actor.

## Event-driven actors ##

_Event-driven actors_ share a **pool** of threads, which are dynamically assigned to actors when the actors need to **react** to messages sent to them. The threads are returned to back the pool once a message has been processed and the actor is idle waiting for some more messages to arrive.

For example, this is how you create an actor that prints out all messages that it receives.

```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

def console = actor {
    loop {
        react {
            println it
        }
    }
}
```

Notice the _loop()_ method call, which ensures that the actor doesn't stop after having processed the first message.

Here's an example with a decryptor service, which can decrypt submitted messages and send the decrypted messages back to the originators.

```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

final def decryptor = actor {
    loop {
        react {String message->
            if ('stopService' == message) stop()
            else reply message.reverse()
        }
    }
}.start()

actor {
    decryptor.send 'suonorhcnysa si yvoorG'
    react {
        println 'Decrypted message: ' + it
        decryptor.send 'stopService'
    }
}.start()
```

Here's an example of an actor that waits for up to 30 seconds to receive a message, prints it out and terminates.

```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

def me = actor {
    friend.send('Hi')
    react(10.seconds) {
        //continue conversation
    }
}
me.metaClass.onTimeout = {->friend.send('I see, busy as usual. Never mind.')}
me.start()
```

See more detailed documentation on [Use of event-driven actors](http://code.google.com/p/gparallelizer/wiki/EventDrivenActors)

## Thread-bound actors ##

_Thread-bound actors_ have their own thread, which is always available to receive and process new messages sent to the actor. When waiting for a message to arrive, the thread is blocked.

For example, this is how you create an actor that prints out all messages that it receives.

```
import static org.gparallelizer.actors.Actors.*

def console = Actors.actor {
    println receive()
}
```

Alternatively you can extend one of the Actor's classes and override the act() method.

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
Here's an example of an actor that waits for up to 30 seconds to receive a message, prints it out and terminates. Something I call a one-shot actor.

```
def notifier = Actors.oneShotActor {
    println receive(30.seconds) ?: 'No message'
}
```

Notice the possible alternative way to retrieve the received value from the receive() method - the value is passed as a parameter into a closure.

```
def notifier = Actors.oneShotActor {
    receive(30.seconds) {
        println it?:'No message'
    }
}
```

See more detailed documentation on [Use of thread-bound actors](http://code.google.com/p/gparallelizer/wiki/ThreadBoundActors)

## Sending replies ##

The _reply/replyIfExists_ methods are not only defined on the actors themselves, but also on the messages upon their reception, which is particularly handy when accepting multiple messages in a single call. In such cases _reply()_ invoked on the actor sends a reply to authors of all the currently processed messages, whereas _reply()_ called on messages sends a reply to the author of the particular message only.

```
react {offerA, offerB, offerC ->
    
    //sent to each of the senders
    reply 'Received your kind offer. Now processing it and comparing with others.'

    offerA.reply 'You were the fastest'  //sent to the author of offerA only

    def winnerOffer = [offerA, offerB, offerC].min {it.price}
    winnerOffer.reply 'I accept your reasonable offer'  //sent to the winner only
    ([offerA, offerB, offerC] - [winnerOffer])*.reply 'Maybe next time'  //sent to the loosers only
}
```

The _reply/replyIfExists_ methods work reliably across the actor types, so that event-driven (pooled) actors can send replies to thread-bound actors and vice versa.

```
//a thread-bound actor
final Actor doubler = Actors.actor {
    receive {num ->
        reply 2 * num
    }
}.start()

//an event-driven actor
PooledActors.actor {
    doubler << 10
    doubler << 100
    react {a, b ->
        println "Doubles for 10 and 100 are $a and $b"
    }
}.start()
```

### Ceasing replies ###
Note: Available since the 0.8 version

Enhancing actor objects as well as the messages at run-time to enable sending replies has a performance cost. If such a need arises, individual actors can have reply enhancement disabled by calling the _disableSendingReplies()_ method.
```
final Actor bouncer = Actors.actor {message ->
    disableSendingReplies()
    receive()
    //reply '2'  //not allowed
    //message.reply '2' //not allowed
    message.author << '2'  //possible, if messages hold the sender explicitly is a property ('author' in the example)
}.start()
```
Obviously, the _reply()/replyIfExists()_ methods cannot be used when sending replies is disabled. However, you can still include senders in message explicitly and send replies using the _send()_ method of the senders.
Sending replies can be re-enabled by calling the _enableSendingReplies()_ method.

### Undelivered messages ###

Note: Available since the 0.8 version

Sometimes messages cannot be delivered to the target actor. When special action needs to be taken for undelivered messages, at actor termination all unprocessed messages from its queue have their _onDeliveryError()_ method called. The _onDeliveryError()_ method or closure defined on the message can, for example, send a notification back to the original sender of the message.

```
final AbstractPooledActor me
me = PooledActors.actor {
    def message1 = 1
    def message2 = 2

    message1.metaClass.onDeliveryError = {->
        me << "Could not deliver $delegate"
    }

    message2.metaClass.onDeliveryError = {->
        me << "Could not deliver $delegate"
    }

    actor1 << message1
    actor2 << message1
    ...
}
```

## Joining actors ##
Note: Available since the 0.8 version

Actors provide a _join()_ method to allow callers to wait for the actor to terminate. A variant accepting a timeout is also available. The Groovy _spread-dot_ operator comes in handy when joining multiple actors at a time.

```
def master = new GameMaster().start()
def player = new Player(name: 'Player', server: master).start()

[master, player]*.join()
```

## Fork/Join Pool ##
Actors leverage the standard JDK concurrency library by default. An experimental support for JSR-166y Fork/Join thread pool has been added as a first step towards fully replaceable thread schedulers.
To turn on Fork/Join pools, make sure you have the JSR-166y jar file on the classpath and either set the _gparallelizer.useFJPool_ property to _true_ or set the appropriate constructor parameter when creating an actor group.

Please note that Fork/Join pool only allows **daemon** threads. The _NonDaemonThreadActorGroup_ and _NonDaemonPooledActorGroup_ classes will continue using the standard JDK thread pooling facilities.