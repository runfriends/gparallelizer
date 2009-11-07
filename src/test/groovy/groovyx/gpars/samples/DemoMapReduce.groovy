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

/**
 * Demonstrates several parallel map/reduce algorithms using the Parallelizer class and leveraging the underlying parallel array library.
 * Requires the jsr166y jar on the class path.
 */

package groovyx.gpars.samples

import groovyx.gpars.Parallelizer

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

Parallelizer.doParallel {
    assert 15 == [1, 2, 3, 4, 5].parallel.reduce {a, b -> a + b}                                        //summarize
    assert 55 == [1, 2, 3, 4, 5].parallel.map {it ** 2}.reduce {a, b -> a + b}                          //summarize squares
    assert 20 == [1, 2, 3, 4, 5].parallel.filter {it % 2 == 0}.map {it ** 2}.reduce {a, b -> a + b}     //summarize squares of even numbers
    assert 20 == (1..5).parallel.filter {it % 2 == 0}.map {it ** 2}.sum()                               //summarize squares of even numbers using sum
    def n = 2
    println ((1..n).parallel.reduce{a, b -> a * b})


    assert 'abc' == 'abc'.parallel.reduce {a, b -> a + b}                                               //concatenate
    assert 'aa:bb:cc:dd:ee' == 'abcde'.parallel.map {it * 2}.reduce {a, b -> "$a:$b"}                   //concatenate dupplicated characters with separator
    assert 'aa-bb-dd' == 'abcde'.parallel.filter {it != 'e'}.map {it * 2}.filter {it != 'cc'}.reduce {a, b -> "$a-$b"}  //filter out some elements

    def urls = [
            'http://www.jroller.com',
            'http://www.dzone.com',
            'http://www.infoq.com',
            'http://www.theserverside.com'
    ]
    //Number of sites mentioning Groovy
    println 'Number of groovy sites today: ' + urls.parallel.map {it.toURL().text.toUpperCase()}.filter {it.contains('GROOVY')}.collection.size()
    println 'Number of words on the longest groovy site today: ' + urls.parallel.map {it.toURL().text.toUpperCase()}.filter {it.contains('GROOVY')}.map{it.split().size()}.max()
    println 'Number of occurencies of the word GROOVY today: ' + urls.parallel.map {it.toURL().text.toUpperCase()}.filter {it.contains('GROOVY')}
            .map{it.split()}.map{it.findAll{word -> word.contains 'GROOVY'}.size()}.sum()

    /**
     * Passing a map around to remember the original url along the way
     */
    final def shortestSite = urls.parallel.map {[url: it, content: it.toURL().text.toUpperCase()]}
            .filter {it.content.contains('GROOVY')}
            .map {[url: it.url, length: it.content.split().size()]}
            .min {a, b -> a.length - b.length}
    println "The shortest groovy site today: $shortestSite.url has $shortestSite.length tokens"

    /**
     * Include an invalid url to show exception handling
     */
    urls << 'http://www.infoqaaaaaaaaaa.com'

    println('Number of groovy sites today: ' + urls.parallel.map {
        try {
            it.toURL().text.toUpperCase()
        } catch (Exception e) {
            e
        }
    }.filter {
        !(Exception.isCase(it)) && it.contains('GROOVY')
    }.collection.size())


}


