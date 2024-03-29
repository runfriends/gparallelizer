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

package groovyx.gpars.samples.actors

import groovyx.gpars.actor.AbstractPooledActor

/**
 * A guess game. A player actor guesses a number and the master replies with either 'too large', 'too small' or 'guessed'.
 * The player continues guessing until he guesses the correct number.
 * @author Jordi Campos i Miralles, Departament de Matematica Aplicada i Analisi, MAiA Facultat de Matematiques, Universitat de Barcelona
 */
class GameMaster extends AbstractPooledActor {
    int secretNum

    void afterStart() {
        secretNum = new Random().nextInt(20)
    }

    void act() {
        loop
        {
            react {int num ->
                if (num > secretNum)
                    reply 'too large'
                else if (num < secretNum)
                    reply 'too small'
                else {
                    reply 'you win'
                    stop()
                }
            }
        }
    }
}

class Player extends AbstractPooledActor {
    String name
    AbstractPooledActor server
    int myNum

    void act() {
        loop
        {
            myNum = new Random().nextInt(20)

            server << myNum

            react {
                switch (it) {
                    case 'too large': println "$name: $myNum was too large"; break
                    case 'too small': println "$name: $myNum was too small"; break
                    case 'you win': println "$name: I won $myNum"; stop(); break
                }
            }
        }
    }
}

final def master = new GameMaster().start()
final def player = new Player(name: 'Player', server: master).start()


[master, player]*.join()
