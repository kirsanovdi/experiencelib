import java.util.*
import kotlin.math.abs

/**Элемент с (k+n)%k позиции */
fun List<Point>.getFromPos(index: Int): Point = this[(this.size + index) % this.size]

/**Предпоследниий элемент стэка */
fun Stack<Point>.previous(): Point = this[this.size - 2]

/**Аппросимация к 0 */
fun toZero(a: Double, precision: Double) = if (abs(a) < precision) 0.0 else a

/**Квадрат числа */
fun sqr(value: Double) = value*value