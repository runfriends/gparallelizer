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

package groovyx.gpars.issues

class SampleA {
    private void foo() {
        println 'Original foo ' + receive('')
    }

    private String bar() {
        "Bar"
    }

    protected Object receive() {
        return "Message " + bar()
    }

    protected Object receive(Object param) {
        receive() + param
    }

    public void perform() {
        foo()
        foo()
    }
}

class SampleB {}

SampleB.metaClass {
    mixin SampleA

    foo = {->
        println 'New foo ' + receive('')
    }
}

final SampleA a = new SampleA()
a.perform()

final SampleB b = new SampleB()
b.perform()

