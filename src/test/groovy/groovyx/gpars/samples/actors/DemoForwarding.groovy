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

import static groovyx.gpars.actor.Actors.actor

/**
 * A demo showing the ability of actors to specify a target actor to receive future replies. By default replies are sent
 * to the originator (sender) of each message, however, when a different actor is specified as the optional second argument
 * to the send() method, this supplied actor will receive the replies instead.
 *
 * @author Vaclav Pech
 * Date: Nov 21, 2009
 */

def decryptor = actor {
    react {message ->
        reply message.reverse()
//        message.reply message.reverse()  //Alternatives to send replies
//        message.sender.send message.reverse()
    }
}

def console = actor {
    react {
        println 'Decrypted message: ' + it
    }
}

decryptor.send 'lellarap si yvoorG', console  //Specify an actor to send replies to

[decryptor, console]*.join()
