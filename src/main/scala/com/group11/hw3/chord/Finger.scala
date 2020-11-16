package com.group11.hw3.chord

import akka.actor.typed.Behavior
import com.group11.hw3.NodeCommand


case class Finger(start: Int, node: Behavior[NodeCommand]  ) {

}
