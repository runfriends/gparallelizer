package org.gparallelizer.samples.actors

import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.Actor

/**
 * Shows solution to the popular Sleeping Barber concurrency problem - http://en.wikipedia.org/wiki/Sleeping_barber_problem
 */

final def group = new PooledActorGroup()

final def barber = group.actor {
    final def random = new Random()
    loop {
        react {message ->
            switch (message) {
                case Enter:
                    message.customer.send new Start()
                    println "Barber: Processing customer ${message.customer.name}"
                    doTheWork(random)
                    message.customer.send new Done()
                    message.reply new Next()
                    break
                case Wait:
                    println "Barber: No customers. Going to have a sleep"
                    break
            }
        }
    }
}.start()

private def doTheWork(Random random) {
    Thread.sleep(random.nextInt(10) * 1000)
}

final Actor waitingRoom

waitingRoom = group.actor {
    final int capacity = 5
    final List<Customer> waitingCustomers = []
    boolean barberAsleep = true

    loop {
        react {message ->
            switch (message) {
                case Enter:
                    if (waitingCustomers.size() == capacity) {
                        reply new Full()
                    } else {
                        waitingCustomers << message.customer
                        if (barberAsleep) {
                            assert waitingCustomers.size() == 1
                            barberAsleep = false
                            waitingRoom.send new Next()
                        }
                        else reply new Wait()
                    }
                    break
                case Next:
                    if (waitingCustomers.size()>0) {
                        def customer = waitingCustomers.remove(0)
                        barber.send new Enter(customer:customer)
                    } else {
                        barber.send new Wait()
                        barberAsleep = true
                    }
            }
        }
    }

}.start()

class Customer extends AbstractPooledActor {
    String name
    Actor localBarbers

    void act() {
        localBarbers << new Enter(customer:this)
        loop {
            react {message ->
                switch (message) {
                    case Full:
                        println "Customer: $name: The waiting room is full. I am leaving."
                        stop()
                        break
                    case Wait:
                        println "Customer: $name: I will wait."
                        break
                    case Start:
                        println "Customer: $name: I am now being served."
                        break
                    case Done:
                        println "Customer: $name: I have been served."
                        break

                }
            }
        }
    }
}

class Enter { Customer customer }
class Full {}
class Wait {}
class Next {}
class Start {}
class Done {}

new Customer(name:'Joe', localBarbers:waitingRoom).start()
new Customer(name:'Dave', localBarbers:waitingRoom).start()
new Customer(name:'Alice', localBarbers:waitingRoom).start()

System.in.read()
new Customer(name:'James', localBarbers:waitingRoom).start()
System.in.read()