interface Drawable {
    fun draw()
}

class Circle(val radius: Int) : Drawable {
    override fun draw() {
        println("  ***  ")
        println(" * * * ")
        println("  ***  ")
        println("Circle with radius: $radius")
    }
}

class Square(val sideLength: Int) : Drawable {
    override fun draw() {
        val line = "*".repeat(sideLength)
        for (i in 1..sideLength) {
            println(line)
        }
        println("Square with side length: $sideLength")
    }
}

fun main() {
    val shapes: List<Drawable> = listOf(
        Circle(5),
        Square(4)
    )

    shapes.forEach {
        it.draw()
        println()
    }
}
