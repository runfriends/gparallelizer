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

package groovyx.gpars.remote;

import groovy.lang.Closure;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.ActorGroup;
import groovyx.gpars.actor.PooledActorGroup;
import groovyx.gpars.scheduler.DefaultPool;
import groovyx.gpars.scheduler.Pool;
import groovyx.gpars.serial.SerialHandles;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Representation of local node
 *
 * @author Alex Tkachman
 */
public class LocalNode {
    private final List<RemoteNodeDiscoveryListener> listeners = Collections.synchronizedList(new LinkedList<RemoteNodeDiscoveryListener>());

    private final ThreadPoolExecutor scheduler;

    private final Actor mainActor;

    private final ActorGroup actorGroup;

    private final UUID id = UUID.randomUUID();

    private LocalHost localHost;

    public LocalNode() {
        this(null, null);
    }

    public LocalNode(Runnable runnable) {
        this(null, runnable);
    }

    public LocalNode(LocalHost provider) {
        this(provider, null);
    }

    public LocalNode(LocalHost provider, Runnable runnable) {
        this.scheduler = new ThreadPoolExecutor(1, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(100),
                new ThreadFactory() {
                    ThreadFactory threadFactory = Executors.defaultThreadFactory();

                    public Thread newThread(Runnable r) {
                        final Thread thread = threadFactory.newThread(r);
                        thread.setDaemon(true);
                        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                            public void uncaughtException(final Thread t, final Throwable e) {
                                System.err.println(Pool.UNCAUGHT_EXCEPTION_OCCURED_IN_ACTOR_POOL + t.getName());
                                e.printStackTrace(System.err);
                            }
                        });
                        return thread;
                    }
                });

        actorGroup = new PooledActorGroup(new DefaultPool(scheduler));

        if (runnable != null) {
            if (runnable instanceof Closure) {
                ((Closure) runnable).setDelegate(this);
                ((Closure) runnable).setResolveStrategy(Closure.DELEGATE_FIRST);
            }
            mainActor = actorGroup.actor(runnable);
        } else {
            mainActor = null;
        }

        localHost = provider;
        if (runnable != null) {
            connect(provider);
        }
    }

    public void connect() {
        if (localHost != null) {
            connect(localHost);
        } else {
            LocalHostRegistry.connect(this);
        }
    }

    public void connect(final LocalHost provider) {
        scheduler.execute(new Runnable() {
            public void run() {
                provider.connect(LocalNode.this);
            }
        });
    }

    public void disconnect() {
        if (mainActor != null && mainActor.isActive()) {
            mainActor.stop();
        }

        if (localHost == null) {
            LocalHostRegistry.disconnect(this);
        } else {
            scheduler.execute(new Runnable() {
                public void run() {
                    localHost.disconnect(LocalNode.this);
                }
            });
        }
    }

    public void addDiscoveryListener(RemoteNodeDiscoveryListener l) {
        listeners.add(l);
    }

    public void addDiscoveryListener(final Closure l) {
        listeners.add(new RemoteNodeDiscoveryListener.RemoteNodeDiscoveryListenerClosure(l));
    }

    public void removeDiscoveryListener(RemoteNodeDiscoveryListener l) {
        listeners.remove(l);
    }

    public void onConnect(final RemoteNode node) {
        for (final RemoteNodeDiscoveryListener l : listeners) {
            scheduler.execute(new Runnable() {
                public void run() {
                    l.onConnect(node);
                }
            });
        }
    }

    public void onDisconnect(final RemoteNode node) {
        for (final RemoteNodeDiscoveryListener l : listeners) {
            scheduler.execute(new Runnable() {
                public void run() {
                    l.onDisconnect(node);
                }
            });
        }
    }

    public Actor getMainActor() {
        return mainActor;
    }

    public Executor getScheduler() {
        return scheduler;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId().toString();
    }

    public SerialHandles getLocalHost() {
        return localHost;
    }
}