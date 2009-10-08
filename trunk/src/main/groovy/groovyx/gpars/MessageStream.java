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
package groovyx.gpars;

import groovy.lang.Closure;
import groovy.time.Duration;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.ActorMessage;
import groovyx.gpars.dataflow.DataCallback;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.RemoteSerialized;
import groovyx.gpars.serial.SerialMsg;
import groovyx.gpars.serial.WithSerialId;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Stream of abstract messages
 *
 * @author Alex Tkachman, Vaclav Pech
 */
public abstract class MessageStream extends WithSerialId {
  /**
   * Send message to stream and return immediately
   *
   * @param message message to send
   * @return always return message stream itself
   */
  public abstract MessageStream send(Object message);

  /**
   * Send message to stream and return immediately
   *
   * @param message message to send
   * @param replyTo where to send reply
   * @param <T>     type of message accepted by the stream
   * @return always return message stream itself
   */
  public final <T> MessageStream send(final T message, final MessageStream replyTo) {
    return send(new ActorMessage<T>(message, replyTo));
  }

  /**
   * Same as send
   *
   * @param message to send
   * @return original stream
   */
  public final <T> MessageStream leftShift(final T message) {
    return send(message);
  }

  /**
   * Sends a message and waits for a reply.
   * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
   *
   * @param message message to send
   * @return The message that came in reply to the original send.
   * @throws InterruptedException if interrupted while waiting
   */
  public final <T, V> V sendAndWait(final T message) throws InterruptedException {
    final ResultWaiter<V> to = new ResultWaiter<V>();
    send(new ActorMessage<T>(message, to));
    return to.getResult();
  }

  /**
   * Sends a message and execute continuation when reply became available.
   *
   * @param message message to send
   * @param closure closure to execute when reply became available
   * @return The message that came in reply to the original send.
   * @throws InterruptedException if interrupted while waiting
   */
  public final <T> MessageStream sendAndContinue(T message, Closure closure) throws InterruptedException {
    closure = (Closure) closure.clone();
    closure.setDelegate(this);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    return send(message, new DataCallback(closure));
  }

  /**
   * Sends a message and waits for a reply. Timeouts after the specified timeout. In case of timeout returns null.
   * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
   *
   * @param message message to send
   * @param timeout timeout
   * @param units   units
   * @return The message that came in reply to the original send.
   * @throws InterruptedException if interrupted while waiting
   */
  public final <T> Object sendAndWait(long timeout, TimeUnit units, T message) throws InterruptedException {
    ResultWaiter to = new ResultWaiter();
    send(new ActorMessage<T>(message, to));
    return to.getResult(timeout, units);
  }

  /**
   * Sends a message and waits for a reply. Timeouts after the specified timeout. In case of timeout returns null.
   * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
   *
   * @param duration timeout
   * @param message  message to send
   * @return The message that came in reply to the original send.
   * @throws InterruptedException if interrupted while waioting
   */
  public final <T> Object sendAndWait(Duration duration, T message) throws InterruptedException {
    return sendAndWait(duration.toMilliseconds(), TimeUnit.MILLISECONDS, message);
  }

  @Override
  public Class<RemoteMessageStream> getRemoteClass() {
    return RemoteMessageStream.class;
  }

  /**
   * Represents a pending request for a reply from an actor.
   *
   * @param <V> The type of expected reply message
   */
  private static class ResultWaiter<V> extends MessageStream {

    /**
     * Holds a reference to the calling thread, while waiting, and the received reply message, once it has arrived.
     */
    private volatile Object value;

    private volatile boolean isSet;

    private ResultWaiter() {
      value = Thread.currentThread();
    }

    /**
     * Accepts the message as a reply and wakes up the sleeping thread.
     *
     * @param message message to send
     * @return this
     */
    @Override
    public MessageStream send(final Object message) {
      final Thread thread = (Thread) this.value;
      if (message instanceof ActorMessage) {
        this.value = ((ActorMessage) message).getPayLoad();
      } else {
        this.value = message;
      }
      isSet = true;
      LockSupport.unpark(thread);
      return this;
    }

    /**
     * Retrieves the response blocking until a message arrives
     *
     * @return The received message
     * @throws InterruptedException If the thread gets interrupted
     */
    public V getResult() throws InterruptedException {
      while (!isSet) {
        LockSupport.park();
        final Thread thread = Thread.currentThread();
        if (thread.isInterrupted()) {
          throw new InterruptedException();
        }
      }
      rethrowException();
      return (V) value;
    }

    /**
     * Retrieves the response blocking until a message arrives
     *
     * @param timeout How long to wait
     * @param units   Unit for the timeout
     * @return The received message
     * @throws InterruptedException If the thread gets interrupted
     */
    public Object getResult(final long timeout, final TimeUnit units) throws InterruptedException {
      final long endNano = System.nanoTime() + units.toNanos(timeout);
      while (!isSet) {
        final long toWait = endNano - System.nanoTime();
        if (toWait <= 0L) {
          return null;
        }
        LockSupport.parkNanos(toWait);
        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException();
        }
      }
      rethrowException();
      return value;
    }

    private void rethrowException() {
      if (value instanceof Throwable) {
        if (value instanceof RuntimeException) {
          throw (RuntimeException) value;
        } else {
          throw new RuntimeException((Throwable) value);
        }
      }
    }

    /**
     * Handle cases when the message sent to the actor doesn't get deliverred
     */
    public void onDeliveryError() {
      send(new IllegalStateException("Delivery error. Maybe target actor is not active"));
    }
  }

  public static class RemoteMessageStream extends MessageStream implements RemoteSerialized {
    private RemoteHost remoteHost;

    public RemoteMessageStream(RemoteHost host) {
      remoteHost = host;
    }

    public MessageStream send(Object message) {
      if (!(message instanceof ActorMessage)) {
        message = new ActorMessage<Object>(message, Actor.threadBoundActor());
      }
      remoteHost.write(new SendTo(this, (ActorMessage) message));
      return this;
    }
  }

  public static class SendTo<T> extends SerialMsg {
    private final MessageStream to;
    private final ActorMessage<T> message;

    public SendTo(MessageStream to, ActorMessage<T> message) {
      super();
      this.to = to;
      this.message = message;
    }

    public MessageStream getTo() {
      return to;
    }

    public ActorMessage<T> getMessage() {
      return message;
    }

    @Override
    public void execute(RemoteConnection conn) {
      to.send(message);
    }
  }
}
