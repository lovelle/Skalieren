/* Just Parking example */
import scala.collection.mutable.ListBuffer

class Parking() {

    val levels = Map(
        "A" -> ListBuffer.fill(10)(0),
        "B" -> ListBuffer.fill(10)(0),
        "C" -> ListBuffer.fill(10)(0)
    )

    def leave (): Boolean = {
        process(-1, 0)
    }

    def enter (): Boolean = {
        process(0, -1)
    }

    private def process(evaluate : Int, setter : Int): Boolean = {
        levels.foreach { l =>
            val level = l._1
            val (const, myindex) = index(level, evaluate)
            if (const == true) {
                levels{level}(myindex) = setter
                return true
            }
        }
        false
    }

    private def index (level : String, evaluate : Int):(Boolean, Int) = {
        levels{level}.foreach { i =>
            if (i == evaluate) {
                return (true, levels{level}.indexOf(i))
            }
        }
        (false, -1)
    }

}

class Building(val action : String) extends Parking {
    def process (): Boolean = {
        if (this.action == "in") this.enter else this.leave
    }
}

class Vehicle(override val action : String) extends Building(action)


/* Main */
object Parking {
    def main(args: Array[String]) {
        val action = "in"
        val parking = new Vehicle(action)

        (0 to 29).foreach { x =>
            if (parking.process == false) {
                print ("Could not perform your ")
                println( if (action == "in") "enter" else "leave")
            } else {
                print("Your ")
                print( if (action == "in") "enter" else "leave")
                println(" was succesful processed")
            }
            println(parking.levels)
        }
    }
}
