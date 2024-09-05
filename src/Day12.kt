import kotlin.math.abs

fun main() {
	data class Axis(var pos: Int, var vel: Int)

	data class Planet(val x: Axis, val y: Axis, val z: Axis) {
		fun energy() = (abs(x.pos) + abs(y.pos) + abs(z.pos)) * (abs(x.vel) + abs(y.vel) + abs(z.vel))
		override fun toString() = "pos=<${x.pos}, ${y.pos}, ${z.pos}>, vel=<${x.vel}, ${y.vel}, ${z.vel}>"
	}

	fun parse(input: List<String>): List<Planet> {
		val planets = mutableListOf<Planet>()
		for (line in input) {
			val (x, y, z) = Regex("<x=(-?\\d+),\\s*y=(-?\\d+),\\s*z=(-?\\d+)>")
				.matchEntire(line)!!.groupValues.takeLast(3).map { it.toInt() }
			planets.add(Planet(Axis(x, 0), Axis(y, 0), Axis(z, 0)))
		}
		return planets
	}

	fun applyGravity(a1: Axis, a2: Axis) {
		if (a1.pos < a2.pos) {
			a1.vel++
			a2.vel--
		} else if (a2.pos < a1.pos) {
			a1.vel--
			a2.vel++
		}
	}

	fun doStep(planets: List<Planet>) {
		// Apply gravity
		for ((i, p1) in planets.withIndex()) {
			for (p2 in planets.subList(i + 1, planets.size)) {
				applyGravity(p1.x, p2.x)
				applyGravity(p1.y, p2.y)
				applyGravity(p1.z, p2.z)
			}
		}
		// Apply velocity
		for (p in planets) {
			p.x.pos += p.x.vel
			p.y.pos += p.y.vel
			p.z.pos += p.z.vel
		}
	}

	fun doStep(planetsAxis: List<Axis>) {
		// Apply gravity
		for ((i, a1) in planetsAxis.withIndex()) {
			for (a2 in planetsAxis.subList(i + 1, planetsAxis.size)) {
				applyGravity(a1, a2)
			}
		}
		// Apply velocity
		for (a in planetsAxis) {
			a.pos += a.vel
		}
	}

	fun part1(input: List<String>, steps: Int = 1000): Int {
		val planets = parse(input)
		for (step in 0 until steps) {
			doStep(planets)
		}
		return planets.sumOf { it.energy() }
	}

	fun matchSteps(initState: List<Axis>): Long {
		val state = initState.map { it.copy() }
		var step = 0L
		do {
			doStep(state)
			step++
		} while(state != initState)
		return step
	}

	fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)

	fun lcm(vararg numbers: Long): Long = numbers.reduce { x, y -> x * (y / gcd(x, y)) }

	fun part2(input: List<String>): Long {
		val planets = parse(input)
		val xSteps = matchSteps(planets.map { it.x })
		val ySteps = matchSteps(planets.map { it.y })
		val zSteps = matchSteps(planets.map { it.z })
		return lcm(xSteps, ySteps, zSteps)
	}

	val testInput = readInput("Day12_test")
	val testInput2 = readInput("Day12_test2")
	check(part1(testInput, 10) == 179)
	check(part1(testInput2, 100) == 1940)
	check(part2(testInput) == 2772L)
	check(part2(testInput2) == 4686774924)

	val input = readInput("Day12")
	part1(input).println()
	part2(input).println()
}
