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

package groovyx.gpars

import groovyx.gpars.scheduler.FJPool
import groovyx.gpars.scheduler.Pool

/**
 * ParallelEnhancer allows classes or instances to be enhanced with parallel variants of iterative methods,
 * like eachParallel(), collectParallel(), findAllParallel() and others. These operations split processing into multiple
 * concurrently executable tasks and perform them on the underlying instance of the ForkJoinPool class from JSR-166y.
 * The pool itself is stored in a final property threadPool and can be managed through static methods
 * on the ParallelEnhancer class.
 * All enhanced classes and instances will share the underlying pool. Use the getThreadPool() method to get hold of the thread pool.
 *
 * @author Vaclav Pech
 * Date: Jun 15, 2009
 */
public final class ParallelEnhancer {

    /**
     * Holds the internal ForkJoinPool instance wrapped into a FJPool
     */
    private final static FJPool threadPool = new FJPool()

    /**
     * Enhances a single instance by mixing-in an instance of ParallelEnhancer.
     */
    public static void enhanceInstance(Object collection) {
        //noinspection GroovyGetterCallCanBePropertyAccess
        collection.getMetaClass().mixin ParallelEnhancer
    }

    /**
     * Enhances a class and so all instances created in the future by mixing-in an instance of ParallelEnhancer.
     * Enhancing classes needs to be done with caution, since it may have impact in unrelated parts of the application.
     */
    public static void enhanceClass(Class clazz) {
        //noinspection GroovyGetterCallCanBePropertyAccess
        clazz.getMetaClass().mixin ParallelEnhancer
    }

    /**
     * Retrieves the underlying pool
     */
    public static Pool getThreadPool() { return threadPool }

    /**
     * Iterates over a collection/object with the <i>each()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and all the potential shared resources have been updated
     * by the threads.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def eachParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.eachParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Iterates over a collection/object with the <i>eachWithIndex()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and all the potential shared resources have been updated
     * by the threads.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def eachWithIndexParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.eachWithIndexParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Iterates over a collection/object with the <i>collect()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     * */
    public def collectParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.collectParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>findAll()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def findAllParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.findAllParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>grep()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def grepParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.grepParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>find()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def findParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.findParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>all()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public boolean allParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.allParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>any()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public boolean anyParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.anyParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>groupBy()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def groupByParallel(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.groupAsync(mixedIn[Object], cl)
        }
    }
}
