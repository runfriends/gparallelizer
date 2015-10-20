## Usage of Asynchronizer ##

The _Asynchronizer_ class enables a Java's Executor Service-based DSL on collections and closures. At least Java 1.5 must be used.

Examples of use:
```
 //multiply numbers asynchronously
 Asynchronizer.withAsynchronizer(5) {
     Collection<Future> result = [1, 2, 3, 4, 5].collectAsync{it * 10}
     assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result*.get()))
 }

 //multiply numbers asynchronously using an asynchronous closure
 Asynchronizer.withAsynchronizer(5) {
     def closure={it * 10}
     def asyncClosure=closure.async()
     Collection<Future> result = [1, 2, 3, 4, 5].collect(asyncClosure)
     assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result*.get()))
 }
```

The passed-in closure takes an instance of a ExecutorService as a parameter, which can be then used freely inside the closure.
```
 //find an element meeting specified criteria
 Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     service.submit({performLongCalculation()} as Runnable)
 }
```
The _Asynchronizer.withAsynchronizer()_ method takes optional parameters for number of threads in the created pool and a thread factory. The _Asynchronizer.withExistingAsynchronizer()_ takes an already existing executor service to reuse. The DSL is valid only within the associated block of code and only for the thread that has called the _withAsynchronizer()_ or _withExistingAsynchronizer()_ method. The _withAsynchronizer()_ method returns only after all the worker threads have finished their tasks and the executor service has been destroyed, returning back the return value of the associated block of code. The _withExistingAsynchronizer()_ method doesn't wait for the executor service threads to finish.

Alternatively, the _Asynchronizer_ class can be statically imported _import static org.gparallelizer.Asynchronizer.`*`_, which will allow omitting the _Asynchronizer_ class name.
```
 withAsynchronizer {
     def result = [1, 2, 3, 4, 5].findAsync{Number number -> number > 2}
     assert result in [3, 4, 5]
 }
```

The following methods on all objects, which support iterations in Groovy, are currently supported:
  * eachAsync()
  * collectAsync()
  * findAllAsync()
  * findAsync()
  * allAsync()
  * anyAsync()

### Closures enhancements ###
The following methods are added to closures inside the _Asynchronizer.withAsynchronizer()_ blocks:
  * async() - Creates an asynchronous variant of the supplied closure, which when invoked returns a future for the potential return value
  * callAsync() - Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value,

Example:
```
Asynchronizer.withAsynchronizer() {
    Closure longLastingCalculation = {calculate()}
    Closure fastCalculation = longLastingCalculation.async()
    Future result=fastCalculation()
    //do stuff while calculation performs
    println result.get()
}
```

### Executor Service enhancements ###
The ExecutorService class is enhanced with the << (leftShift) operator to submit tasks to it returning a _Future_ for the result. It wraps the _submit()_ method call on the service.

Example:
```
Asynchronizer.withAsynchronizer {ExecutorService executorService ->
    executorService << {println 'Inside parallel task'}
}
```

### Parallelizing closures ###
The AsyncInvokerUtil class offers methods _doInParallel()_, _executeInParallel()_ and _startInParallel()_ to easily run multiple closures in parallel.

Example:
```
Asynchronizer.withAsynchronizer {
    assertEquals([10, 20], AsyncInvokerUtil.doInParallel({calculateA()}, {calculateB()}))
}
```

### Meta-class enhancer ###
As an alternative since the 0.8 release you can use the _AsyncEnhancer_ class to enhance meta-classes for any classes or individual instances with asynchronous methods.
```
import org.gparallelizer.AsyncEnhancer

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]
AsyncEnhancer.enhanceInstance(list)
println list.collectAsync {it * 2 }

def animals = ['dog', 'ant', 'cat', 'whale']
AsyncEnhancer.enhanceInstance animals
println (animals.anyAsync {it ==~ /ant/} ? 'Found an ant' : 'No ants found')
println (animals.allAsync {it.contains('a')} ? 'All animals contain a' : 'Some animals can live without an a')
```

If exceptions are thrown while processing any of the passed-in closures, an instance of _AsyncException_ wrapping all the original exceptions gets re-thrown from the xxxAsync methods.