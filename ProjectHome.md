**GParallelizer** offers Groovy developers intuitive ways to handle tasks concurrently.

# Project moved and renamed #

The **GParallelizer** project has been renamed to **GPars (Groovy Parallel Systems)** and has moved to http://gpars.codehaus.org

**GParallelizer 0.8.4** is the last official release of gparallelizer under its original name.

# GParallelizer #
[Check out the GParallelizer discussion group](http://groups.google.com/group/gparallelizer)

[Have a look at a couple of examples on GParallelizer use](http://code.google.com/p/gparallelizer/wiki/ActorsExamples)

**GParallelizer** can be easily [integrated using Grape, Gradle or Maven](Integration.md).[A GParallelizer Grails plugin](http://www.grails.org/plugin/gparallelizer) and [a Griffon plugin](http://griffon.codehaus.org/Gparallelizer+Plugin) are also available.

Watch GParallelizer builds online at the [JetBrains public Continuous Integration hosting site](http://teamcity.jetbrains.com/project.html?projectId=project34)

The framework provides straightforward Groovy-based Domain Specific Languages (DSLs) to declare, which parts of the code should be performed in parallel. Objects are enhanced with asynchronous methods like _eachAsync()_, _collectAsync()_ and others, to perform collection-based operations in parallel. Also, closures can be turned into their asynchronous variants, which when invoked schedule the original closure for processing in an executor service. The library also provides several helper methods for running a set of closures concurrently.

Effective Scala-like actors are ready to make your code leverage an inherently safer way to organize concurrent code by eliminating mutable shared state altogether. With actors support you can quickly create several independent Actors, which consume messages passed to them in their own thread and communicate with other actors by sending them messages. You then build your solution by combining these actors into a communication network.

The library has four logical parts.
  1. **Actors** provide a Groovy implementation of Scala-like actors, both thread-bound actors and thread pool-bound (event-driven) ones
  1. **Dataflow Concurrency** (since version 0.8) allows for very natural shared-memory concurrency model, based on single-assignment variables.
  1. **Asynchronizer** uses the Java 1.5 built-in support for executor services to enable multi-threaded collection and closure processing.
  1. **Parallelizer** uses JSR-166y Parallel Arrays to enable multi-threaded collection processing.

## Actors ##

_Actors_ in GParallelizer were inspired by Actors library in Scala. They allow for messaging-based concurrency model, built from independent active objects that exchange messages and have no mutable shared state. Actors naturally avoid issues like deadlocks, livelocks or starvation, so typical for shared memory.

Detailed documentation for [Actors](http://code.google.com/p/gparallelizer/wiki/Actors) is available.

The actors library allows for interesting applications, like [SafeVariable (aka Clojure Agents)](http://code.google.com/p/gparallelizer/wiki/SafeVariables).

Examples of use:
```
import static org.gparallelizer.actors.pooledActors.PooledActors.*

 //create a new actor that prints out received messages
def console = actor {
    loop {
        react {message ->
            println message
        }
    }
}

//start the actor and send it a message
console.start()
console.send('Message')
Thread sleep 1000
```

## Dataflow Concurrency ##

Dataflow concurrency offers an alternative concurrency model, which is inherently safe and robust. The important benefits include:

  * No race-conditions
  * No live-locks
  * Deterministic deadlocks
  * Completely deterministic programs
  * BEAUTIFUL code.

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

Check out the details on [Dataflow Concurrency in GParallelizer](DataflowConcurrency.md).

## Asynchronizer ##

The _Asynchronizer_ class enables a Java's Executor Service-based DSL on collections and closures. Java 1.5 is the minimum requirement.

Detailed documentation for [Asynchronizer](http://code.google.com/p/gparallelizer/wiki/Asynchronizer) is available.

Examples of use:
```
 //multiply numbers asynchronously
 Asynchronizer.withAsynchronizer(5) {
     Collection<Future> result = [1, 2, 3, 4, 5].collectAsync{it * 10}
     assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result*.get()))
 }
```

```
//run multiple closures in parallel
Asynchronizer.withAsynchronizer {
    assertEquals([10, 20], AsyncInvokerUtil.doInParallel({calculateA()}, {calculateB()}))
}
```

## Parallelizer ##

The _Parallelizer_ class enables a ParallelArray-based (from JSR-166y) DSL on collections. In general cases the Parallel Arrays implementation shows to be much faster (10 - 20 times) compared to the executor service implementation in Asynchronizer.

The jsr166y-070108.jar (downloadable from e.g. http://repo1.maven.org/maven2/org/coconut/forkjoin/jsr166y/070108/jsr166y-070108.jar) must be on the classpath.
```
<dependency>
    <groupId>org.coconut.forkjoin</groupId>
    <artifactId>jsr166y</artifactId>
    <version>070108</version>
</dependency>
```

Detailed documentation for [Parallelizer](http://code.google.com/p/gparallelizer/wiki/Parallelizer) is available.

Examples of use:
```
 //multiply numbers asynchronously
 Parallelizer.withParallelizer(5) {
     final List result = [1, 2, 3, 4, 5].collectAsync {it * 2}
     assert ([2, 4, 6, 8, 10].equals(result))
 }
```

## Compatibility ##
GParallelizer should work under JDK 1.6.0\_x and Groovy 1.6.3 and above.
Tested on JDK 1.6.0\_12 and Groovy 1.6.4

## Credits ##

GParallelizer would be half as good and polished as it is without help and advice of many exceptional individuals, who've spend some of their valuable time experimenting, suggesting and discussing concurrency options in Groovy as well as concrete implementation details of GParallelizer.
I'd like to share the credits with at least some of them by listing their names here, without any particular order. Great thanks to these gentlemen:
  * Dierk Koenig
  * Jonas Bonér
  * Guillaume Laforge
  * Wilson MacGyver
  * Jan Kotek
  * Hans Dockter
  * Tom Nichols

### GParallelizer in the news ###
[Java Posse #266](http://javaposse.com/index.php?post_id=501792#)

[GroovyMag July 2009](http://www.groovymag.com/main.issues.description/id=11/)

[The 0.8 release announcement](http://groovy.dzone.com/announcements/gparalelizer-08-released)

[A Domain-Specific Language to Let Groovy Go Parallel by Gaston Hillar](http://www.ddj.com/go-parallel/blog/archives/2009/06/a_domainspecifi.html)

[The 0.7 release announcement](http://www.jroller.com/vaclav/entry/another_milestone_for_gparallelizer_the)

[Actor libraries on JVM comparison by Alex Miller](http://www.javaworld.com/javaworld/jw-03-2009/jw-03-actor-concurrency2.html)

[Event-based actors in Groovy (GR8 Conference)](http://www.gr8conf.org/blog/2009/03/05/19)

[Event-based actors in Groovy (DZone)](http://groovy.dzone.com/news/event-based-actors-groovy)

[Event-based actors in Groovy](http://www.jroller.com/vaclav/entry/event_based_actors_in_groovy)

[Actors in Groovy](http://www.jroller.com/vaclav/entry/groovy_actors_in_gparallelizer_concurrency)

[GParallelizer announced](http://www.jroller.com/vaclav/entry/gparallelizer_made_available)
### Feedback ###
Any feedback or source contribution is appreciated. Don't hesitate to ask for help or support if you need one. Please e-mail me to gparallelizer (located at) gmail (dot) com.