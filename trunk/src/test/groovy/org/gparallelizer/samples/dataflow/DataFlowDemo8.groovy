package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.thread

DataFlowActor.DATA_FLOW_GROUP.threadPool.resize 10

final def mass = new DataFlowVariable()
final def radius = new DataFlowVariable()
final def volume = new DataFlowVariable()
final def density = new DataFlowVariable()
final def acceleration = new DataFlowVariable()
final def time = new DataFlowVariable()
final def velocity = new DataFlowVariable()
final def decelerationForce = new DataFlowVariable()
final def deceleration = new DataFlowVariable()
final def distance = new DataFlowVariable()

thread {
    println """
Calculating distance required to stop a moving ball.
====================================================
The ball has a radius of ${~radius} meters and is made of a material with ${~density} kg/m3 density,
which means that the ball has a volume of ${~volume} m3 and a mass of ${~mass} kg.
The ball has been accelerating with ${~acceleration} m/s2 from 0 for ${~time} seconds and so reached a velocity of ${~velocity} m/s.

Given our ability to push the ball backwards with a force of ${~decelerationForce} N (Newton), we can cause a deceleration
of ${~deceleration} m/s2 and so stop the ball at a distance of ${~distance} m.

=======================================================================================================================
This example has been calculated asynchronously in multiple threads using GParallelizer DataFlow concurrency in Groovy.
"""

    System.exit 0
}

thread {
    mass << ~volume * ~density
}

thread {
    volume << Math.PI * (~radius ** 3)
}

thread {
    radius << 2.5
    density << 	998.2071  //water
    acceleration << 9.80665 //free fall
    decelerationForce << 900
}

thread {
    time << 10
    velocity << ~acceleration * ~time
}

thread {
    deceleration << ~decelerationForce / ~mass
}

thread {
    distance << ~deceleration * ((~velocity/~deceleration) ** 2) * 0.5
}

System.in.read()