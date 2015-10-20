## Usage of Parallelizer ##

The _Parallelizer_ class enables a ParallelArray-based (from JSR-166y) DSL on collections. In general cases the Parallel Arrays implementation shows to be much faster (10 - 20 times) compared to the executor service implementation in Asynchronizer.

The jsr166y-070108.jar (downloadable from e.g. http://repo1.maven.org/maven2/org/coconut/forkjoin/jsr166y/070108/jsr166y-070108.jar) must be on the classpath.

```
<dependency>
    <groupId>org.coconut.forkjoin</groupId>
    <artifactId>jsr166y</artifactId>
    <version>070108</version>
</dependency>
```

Examples of use:
```
 //summarize numbers concurrently
 Parallelizer.withParallelizer(5) {
     final AtomicInteger result = new AtomicInteger(0)
     [1, 2, 3, 4, 5].eachAsync {result.addAndGet(it)}
     assertEquals 15, result
 }

 //multiply numbers asynchronously
 Parallelizer.withParallelizer(5) {
     final List result = [1, 2, 3, 4, 5].collectAsync {it * 2}
     assert ([2, 4, 6, 8, 10].equals(result))
 }
```
The passed-in closure takes an instance of a ForkJoinPool as a parameter, which can be then used freely inside the closure.
```
 //check whether all elements within a collection meet certain criteria
 Parallelizer.withParallelizer(5) {ForkJoinPool pool ->
     assert [1, 2, 3, 4, 5].allAsync {it > 0}
     assert ![1, 2, 3, 4, 5].allAsync {it > 1}
 }
```
The _Parallelizer.withParallelizer()_ method takes optional parameters for number of threads in the created pool and unhandled exception handler. The _Parallelizer.withExistingParallelizer()_ takes an already existing thread pool to reuse.
The DSL is valid only within the associated block of code and only for the thread that has called the _withParallelizer()_ or _withExistingParallelizer()_ method. The _withParallelizer()_ method returns only after all the worker threads have finished their tasks and the pool has been destroyed, returning back the return value of the associated block of code. The _withExistingParallelizer()_ method doesn't wait for the pool threads to finish.

Alternatively, the _Parallelizer_ class can be statically imported _import static org.gparallelizer.Parallelizer.`*`_, which will allow omitting the _Parallelizer_ class name.
```
 withParallelizer {
     assert [1, 2, 3, 4, 5].allAsync {it > 0}
     assert ![1, 2, 3, 4, 5].allAsync {it > 1}
 }
```
The following methods on collections are currently supported:
  * eachAsync()
  * collectAsync()
  * findAllAsync()
  * findAsync()
  * allAsync()
  * anyAsync()

### Meta-class enhancer ###
As an alternative since the 0.8 release you can use the _ParallelEnhancer_ class to enhance meta-classes for any classes or individual instances with asynchronous methods.
```
import org.gparallelizer.ParallelEnhancer

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]
ParallelEnhancer.enhanceInstance(list)
println list.collectAsync {it * 2 }

def animals = ['dog', 'ant', 'cat', 'whale']
ParallelEnhancer.enhanceInstance animals
println (animals.anyAsync {it ==~ /ant/} ? 'Found an ant' : 'No ants found')
println (animals.allAsync {it.contains('a')} ? 'All animals contain a' : 'Some animals can live without an a')
```

If an exception is thrown while processing any of the passed-in closures, the exception gets re-thrown from the xxxAsync methods.