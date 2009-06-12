package org.gparallelizer.actors.pooledActors

import groovy.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.ActorMessage
import org.gparallelizer.actors.ReplyEnhancer
import org.gparallelizer.actors.pooledActors.*
import static org.gparallelizer.actors.pooledActors.ActorAction.actorAction
import static org.gparallelizer.actors.pooledActors.ActorException.*
import org.gparallelizer.actors.ReplyEnhancer
import java.util.concurrent.CopyOnWriteArrayList


/**
 * AbstractPooledActor provides the default PooledActor implementation. It represents a standalone active object (actor),
 * which reacts asynchronously to messages sent to it from outside through the send() method.
 * Each PooledActor has its own message queue and a thread pool shared with other PooledActors.
 * The work performed by a PooledActor is divided into chunks, which are sequentially submitted as independent tasks
 * to the thread pool for processing.
 * Whenever a PooledActor looks for a new message through the react() method, the actor gets detached
 * from the thread, making the thread available for other actors. Thanks to the ability to dynamically attach and detach
 * threads to actors, PooledActors can scale far beyond the limits of the underlying platform on number of cuncurrently
 * available threads.
 * The loop() method allows repeatedly invoke a closure and yet perform each of the iterations sequentially
 * in different thread from the thread pool.
 * To suport continuations correctly the react() and loop() methods never return.
 * <pre>
 * import static org.gparallelizer.actors.pooledActors.PooledActors.*
 *
 * def actor = actor {*     loop {*         react {message ->
 *             println message
 *}*         //this line will never be reached
 *}*     //this line will never be reached
 *}.start()
 *
 * actor.send 'Hi!'
 * </pre>
 * This requires the code to be structured accordingly.
 *
 * <pre>
 * def adder = actor {*     loop {*         react {a ->
 *             react {b ->
 *                 println a+b
 *                 replyIfExists a+b  //sends reply, if b was sent by a PooledActor
 *}*}*         //this line will never be reached
 *}*     //this line will never be reached
 *}.start()
 * </pre>
 * The closures passed to the react() method can call reply() or replyIfExists(), which will send a message back to
 * the originator of the currently processed message. The replyIfExists() method unlike the reply() method will not fail
 * if the original message wasn't sent by an actor nor if the original sender actor is no longer running.
 * The react() method accepts timout specified using the TimeCategory DSL.
 * <pre>
 * react(10.MINUTES) {*     println 'Received message: ' + it
 *}* </pre>
 * If not message arrives within the given timeout, the onTimeout() lifecycle handler is invoked, if exists,
 * and the actor terminates.
 * Each PooledActor has at any point in time at most one active instance of ActorAction associated, which abstracts
 * the current chunk of actor's work to perform. Once a thread is assigned to the ActorAction, it moves the actor forward
 * till loop() or react() is called. These methods schedule another ActorAction for processing and throw dedicated exception
 * to terminate the current ActorAction. 
 *
 * @author Vaclav Pech
 * Date: Feb 7, 2009
 */
abstract public class AbstractPooledActor implements PooledActor {

    /**
     * Queue for the messages
     */
    private final BlockingQueue messageQueue = new LinkedBlockingQueue();

    /**
     * Code for the next action
     */
    private final AtomicReference<Closure> codeReference = new AtomicReference<Closure>(null)

    /**
     * Buffer to hold messages required by the scheduled next continuation. Null when no continuation scheduled.
     */
    private MessageHolder bufferedMessages = null

    //todo should be private, but wouldn't work
    /**
     * A copy of buffer in case of timeout.
     */
    volatile List savedBufferedMessages = null

    /**
     * A list of senders for the currently procesed messages
     */
    List senders = []

    //todo should be private, nut wouldn't work
    /**
     * Indicates whether the actor should enhance messages to enable sending replies to their senders
     */
    volatile boolean sendRepliesFlag = true

    /**
     * Indicates whether the actor should terminate
     */
    private final AtomicBoolean stopFlag = new AtomicBoolean(true)

    /**
     * Code for the loop, if any
     */
    private final AtomicReference<Closure> loopCode = new AtomicReference<Closure>(null)

    /**
     * The current active action (continuation) associated with the actor. An action must not use Actor's state
     * after it schedules a new action, only throw CONTINUE.
     */
    final AtomicReference<ActorAction> currentAction = new AtomicReference<ActorAction>(null)

    /**
     * The current timeout task, which will send a TIMEOUT message to the actor if not cancelled by someone
     * calling the send() method within the timeout specified for the currently blocked react() method.
     */
    private AtomicReference<TimerTask> timerTask = new AtomicReference<TimerTask>(null)

    /**
     * Internal lock to synchronize access of external threads calling send() or stop() with the current active actor action
     */
    private final Object lock = new Object()

    /**
     * Timer holding timeouts for react methods
     */
    private static final Timer timer = new Timer(true)

    /**
     * The actor group to which the actor belongs
     */
    volatile PooledActorGroup actorGroup = PooledActors.defaultPooledActorGroup

    /**
     * Indicates whether the actor's group can be changed. It is typically not changeable after actor starts.
     */
    private volatile boolean groupMembershipChangeable = true

    /**
     * Sets the actor's group.
     * It can only be invoked before the actor is started.
     */
    public final void setActorGroup(PooledActorGroup group) {
        if (!groupMembershipChangeable) throw new IllegalStateException("Cannot set actor's group on a started actor.")
        if (!group) throw new IllegalArgumentException("Cannot set actor's group to null.")
        actorGroup = group
    }

    /**
     * Enabled the actor and received messages to have the reply()/replyIfExists() methods called on them.
     * Sending replies is enabled by default.
     */
    final void enableSendingReplies() {
        sendRepliesFlag = true
    }

    /**
     * Disables the actor and received messages to have the reply()/replyIfExists() methods called on them.
     * Calling reply()/replyIfExist() on the actor will result in IllegalStateException being thrown.
     * Calling reply()/replyIfExist() on a received message will result in MissingMethodException being thrown.
     * Sending replies is enabled by default.
     */
    final void disableSendingReplies() {
        sendRepliesFlag = false
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    public final AbstractPooledActor start() {
        groupMembershipChangeable = false
        if (!stopFlag.getAndSet(false)) throw new IllegalStateException("Actor has alredy been started.")
        actorAction(this) {
            if (delegate.respondsTo('afterStart')) delegate.afterStart()
            act()
        }
        return this
    }

    /**
     * Sets the stopFlag
     * @return The previous value of the stopFlag
     */
    final boolean indicateStop() {
        cancelCurrentTimeoutTimer('')
        return stopFlag.getAndSet(true)
    }

    /**
     * Stops the Actor. The background thread will be interrupted, unprocessed messages will be passed to the afterStop
     * method, if exists.
     * Has no effect if the Actor is not started.
     * @return The actor
     */
    public final Actor stop() {
        synchronized (lock) {
            if (!indicateStop()) {
                (codeReference.getAndSet(null)) ? actorAction(this) { throw TERMINATE } : currentAction.get()?.cancel()
            }
        }
        return this
    }

    /**
     * Checks the current status of the Actor.
     */
    public final boolean isActive() {
        return !stopFlag.get()
    }

    /**
     * Checks whether the current thread is the actor's current thread.
     */
    public final boolean isActorThread() {
        return Thread.currentThread() == currentAction.get()?.actionThread
    }

    /**
     * This method represents the body of the actor. It is called upon actor's start and can exit either normally
     * by return or due to actor being stopped through the stop() method, which cancels the current actor action.
     * Provides an extension point for subclasses to provide their custom Actor's message handling code.
     * The default implementation throws UnsupportedOperationException.
     */
    protected void act() {
        throw new UnsupportedOperationException("The act() method must be overriden")
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param duration Time to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * The TimeCategory DSL to specify timeouts is available inside the Actor's act() method.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Duration duration, final Closure code) {
        react(duration.toMilliseconds(), code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Closure code) {
        react(-1, code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param timeout Time in miliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, TimeUnit timeUnit, final Closure code) {
        react(timeUnit.toMillis(timeout), code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * Also adds reply() and replyIfExists() methods to the currentActor and the message.
     * These methods will call send() on the target actor (the sender of the original message).
     * The reply()/replyIfExists() methods invoked on the actor will be sent to all currently processed messages,
     * reply()/replyIfExists() invoked on a message will send a reply to the sender of that particular message only.
     * @param timeout Time in miliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, final Closure code) {

        senders.clear()
        final int maxNumberOfParameters = code.maximumNumberOfParameters

        Closure reactCode = {List<ActorMessage> messages ->

            if (messages.any {ActorMessage actorMessage -> (TIMEOUT == actorMessage.payLoad)}) {
                savedBufferedMessages = messages.findAll {it != null && TIMEOUT != it.payLoad}*.payLoad
                throw TIMEOUT
            }

            if (sendRepliesFlag) {
                for (message in messages) {
                    senders << message?.sender
                }
                ReplyEnhancer.enhanceWithReplyMethodsToMessages(messages)
            }
            maxNumberOfParameters > 0 ? code.call(* (messages*.payLoad)) : code.call()
            this.repeatLoop()
        }

        synchronized (lock) {
            if (stopFlag.get()) throw TERMINATE
            assert (codeReference.get() == null), "Cannot have more react called at the same time."

            bufferedMessages = new MessageHolder(maxNumberOfParameters)

            ActorMessage currentMessage
            while ((!bufferedMessages.ready) && (currentMessage = (ActorMessage) messageQueue.poll())) {
                bufferedMessages.addMessage(currentMessage)
            }
            if (bufferedMessages.ready) {
                final List<ActorMessage> messages = bufferedMessages.messages
                actorAction(this) { reactCode.call(messages) }
                bufferedMessages = null
            } else {
                codeReference.set(reactCode)
                if (timeout >= 0) {
                    timerTask.set([run: { this.send(TIMEOUT) }] as TimerTask)
                    timer.schedule(timerTask.get(), timeout)
                }
            }
        }
        throw CONTINUE
    }

    /**
     * Sends a reply to all currently processed messages. Throws ActorReplyException if some messages
     * have not been sent by an actor. For such cases use replyIfExists().
     * Calling reply()/replyIfExist() on the actor with disabled replying (through the disableSendingReplies() method)
     * will result in IllegalStateException being thrown.
     * Sending replies is enabled by default.
     * @throws ActorReplyException If some of the replies failed to be sent.
     */
    protected final void reply(Object message) {
        assert senders != null
        if (!sendRepliesFlag) throw new IllegalStateException("Cannot send a reply $message. Replies have been disabled.")
        if (!senders.isEmpty()) {
            List<Exception> exceptions = []
            for (sender in senders) {
                if (sender != null) {
                    try { sender.send message } catch (IllegalStateException e) {exceptions << e }
                }
                else exceptions << new IllegalArgumentException("Cannot send a reply message ${message} to a null recipient.")
            }
            if (!exceptions.empty) throw new ActorReplyException('Failed sending some replies. See the issues field for details', exceptions)
        } else {
            throw new ActorReplyException("Cannot send replies. The list of recipients is empty.")
        }
    }

    /**
     * Sends a reply to all currently processed messages, which have been sent by an actor.
     * Ignores potential errors when sending the replies, like no sender or sender already stopped.
     * Calling reply()/replyIfExist() on the actor with disabled replying (through the disableSendingReplies() method)
     * will result in IllegalStateException being thrown.
     * Sending replies is enabled by default.
     */
    protected final void replyIfExists(Object message) {
        assert senders != null
        if (!sendRepliesFlag) throw new IllegalStateException("Cannot send a reply $message. Replies have been disabled.")
        for (sender in senders) {
            try {
                sender?.send message
            } catch (IllegalStateException ignore) { }
        }
    }

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     * If there's no ActorAction scheduled for the actor a new one is created and scheduled on the thread pool.
     */
    public final Actor send(Object message) {
        synchronized (lock) {
            if (stopFlag.get()) throw new IllegalStateException("The actor hasn't been started.");
            cancelCurrentTimeoutTimer(message)

            def actorMessage = ActorMessage.build(message)

            final Closure currentReference = codeReference.get()
            if (currentReference) {
                assert bufferedMessages && !bufferedMessages.isReady()
                bufferedMessages.addMessage actorMessage
                if (bufferedMessages.ready) {
                    final List<ActorMessage> messages = bufferedMessages.messages
                    actorAction(this) { currentReference.call(messages) }
                    codeReference.set(null)
                    bufferedMessages = null
                }
            } else {
                messageQueue.put(actorMessage)
            }
        }
        return this
    }

    /**
     * Sends a message and waits for a reply.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @return The message that came in reply to the original send.
     */
    public sendAndWait(Object message) {
        Actor representative = new SendAndWaitPooledActor(this, message)
        representative.start()
        return representative.result
    }

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     * If there's no ActorAction scheduled for the actor a new one is created and scheduled on the thread pool.
     */
    public final Actor leftShift(Object message) { send message }

    /**
     * Clears the message queue returning all the messages it held.
     * @return The messages stored in the queue
     */
    final List sweepQueue() {
        def messages = []
        if (savedBufferedMessages) messages.addAll savedBufferedMessages
        ActorMessage message = messageQueue.poll()
        while (message != null) {
            if (message.payLoad.respondsTo('onDeliveryError')) message.payLoad.onDeliveryError()
            messages << message
            message = messageQueue.poll()
        }
        return messages
    }

    /**
     * Ensures that the suplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param code The closure to invoke repeatedly
     */
    protected final void loop(final Closure code) {
        assert loopCode.get() == null, "The loop method must be only called once"
        final Closure enhancedCode = {code(); repeatLoop()}
        enhancedCode.delegate = this
        loopCode.set(enhancedCode)
        doLoopCall(enhancedCode)
    }

    /**
     * Plans another loop iteration
     */
    protected final void repeatLoop() {
        final Closure code = loopCode.get()
        if (!code) return;
        doLoopCall(code)
    }

    private def doLoopCall(Closure code) {
        if (stopFlag.get()) throw TERMINATE
        actorAction(this, code)
        throw CONTINUE
    }

    private def cancelCurrentTimeoutTimer(message) {
        if (TIMEOUT != message) timerTask.get()?.cancel()
    }

    //Document before next release
    //todo use @Delegate from group to pool
    //todo try @Immutable messages
    //todo create a performance benchmarks
    //todo loop without react stops after return, otherwise not
    //todo add a fastSend() method to send a (singleton) message, which you never expect replies to
    //todo test the onDeliveryError handler
    //todo add synchronous calls plus an operator

    //Planned for the next release
    //todo timeouts for sendAndWait()

    //todo abandoned actor group - what happens to the pool, senders

    //todo dataflow concurrency - clarify, remove shutdown() and EXIT after SetMessage
    //todo move Java sources and tests to the java folder
    //todo make thread-bound reuse threads to speed-up their creation and make the compatible with Fork/Join
    //todo use ForkJoin

    //Backlog
    //todo use Gradle
    //todo automate code sample download
    //todo automate javadoc download

    //todo maven
    //todo put into maven repo
    //todo add transitive mvn dependencies
    //todo remove type info for speed-up
    //todo ActorAction into Java
    //todo speedup actor creation
    //todo switch each to for loops where helping performance
    //todo reconsider locking in Actors
    //todo consider asynchronous metaclass
    //todo use AST transformation to turn actors methods into async processing
    //todo implement in Java
    //todo unify actors and pooled actors behavior on timeout and exception, (retry after timeout and exception or stop)
    //todo try the fixes for the MixinTest
    //todo consider flow control to throttle message production
    //todo resize the pool if all threads are busy or blocked
    //todo support mixins for event-driven actors

    //To consider
    //todo multiple loops
    //todo exit the current loop
    //todo test on Google App Engine
    //todo consider other types of queues
    //todo actor groups could manage actors and give public access to them
    //todo thread-bound actors could use threads from a pool or share a thread factory
    //todo shorten method names withAsynchronizer and withParallelizer doAsync, doParallel
    //todo add sendLater(Duration) and sendAfterDone(Future)
    //todo consider pass by copy (clone, serialization) for mutable messages, reject mutable messages otherwise
    //todo unify and publish spawn operation and mail boxes
    //todo associate a mail box with each thread, not only with actors
    //todo add generics to actors
    //todo implement remote actors

    /*
import scala.actors.Actor._
import scala.actors.Future

case class Fib(n: Int)
case class Add(a: Future[Int], b: Future[Int])
case class Add2(a: Int, b: Future[Int])

val fib = actor { loop { react {
 case Fib(n) if n <= 2 => reply(1)
 case Fib(n) =>
   val a = self !! (Fib(n-1), { case x => x.asInstanceOf[Int] })
   val b = self !! (Fib(n-2), { case x => x.asInstanceOf[Int] })
   self.forward(Add(a, b))
 case Add(a, b) if a.isSet && b.isSet =>
   reply(a() + b())
 case Add(a, b) if a.isSet =>
   self.forward(Add2(a(), b))
 case Add(a, b) if b.isSet =>
   self.forward(Add2(b(), a))
 case Add(a, b) =>
   self.forward(Add(a, b))
 case Add2(a, b) if b.isSet =>
   reply(a + b())
 case Add2(a, b) =>
   self.forward(Add2(a, b))
} } }
     */
}

final class SendAndWaitPooledActor extends AbstractPooledActor {
    private Actor targetActor
    private Object message
    //todo use Phaser instead once available to keep the thread running
    private CountDownLatch latch = new CountDownLatch(1)
    private Object result

    def SendAndWaitPooledActor(final targetActor, final message) {
        this.targetActor = targetActor;
        this.message = message
    }

    void act() {
        message.getMetaClass().onDeliveryError = {->
            this << new IllegalStateException('Cannot deliver the message. The target actor may not be active.')
        }

        targetActor << message
        react {
            result = it
        }
    }

    void onException(Exception e) {
        result = e
    }

    void afterStop(undeliveredMessages) {
        latch.countDown()
    }

    Object getResult() {
        latch.await()
        if (result instanceof Exception) throw result else return result
    }
}