abstract class Animal(val name: String, val legs: Int) {
    abstract fun makeSound(): String
}

class Dog(name: String) : Animal(name, legs = 4) {
    override fun makeSound() = "Woof!"
}

class Cat(name: String) : Animal(name, legs = 4) {
    override fun makeSound() = "Meow!"
}

fun main() {
    val animals: List<Animal> = listOf(
        Dog("Buddy"),
        Cat("Whiskers")
    )

    for (animal in animals) {
        println("${animal.name} says ${animal.makeSound()}")
    }
}
