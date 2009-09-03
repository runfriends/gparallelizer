package org.gparallelizer.issues

class A {

    int counter = 0

    protected def final foo() {
        bar {
            counter
        }
    }

    private final String bar(Closure code) {
        return "Bar " + code()
    }
}

class B extends A {}

class C {}

C.metaClass {
    mixin B
}

def c= new C()
c.foo()
