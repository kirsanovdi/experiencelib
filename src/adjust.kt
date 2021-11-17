import java.util.*
import kotlin.math.*

/**Дельта для вычислений с плавающей точкой */
const val delta = 1e-10

/**Точка */
data class Point(val x: Double, val y: Double) {
    fun distance(other: Point): Double = sqrt(sqr(x - other.x) + sqr(y - other.y))
    override fun equals(other: Any?) = other is Point && this.x == other.x && this.y == other.y
    override fun hashCode(): Int = x.hashCode() + y.hashCode()
}

/**Отрезок */
data class Segment(val begin: Point, val end: Point) {
    fun length() = begin.distance(end)
    override fun equals(other: Any?) =
        other is Segment && (begin == other.begin && end == other.end || end == other.begin && begin == other.end)
    override fun hashCode() =
        begin.hashCode() + end.hashCode()
}

/**Вектор в двумерном пространстве */
data class ZeroVector(val x: Double, val y: Double) {
    constructor(begin: Point, end: Point) : this(end.x - begin.x, end.y - begin.y)
    private fun length(): Double = sqrt(x * x + y * y)
    fun angleWith(other: ZeroVector): Double =
        acos((this.x * other.x + this.y * other.y) / (this.length() * other.length()))
}

/**Старый метод за N(O^2) */
fun diameterOld(vararg points: Point): Segment {
    if (points.size < 2) throw IllegalArgumentException()
    if (points.size == 2) return Segment(points[0], points[1])
    val list = points.toMutableList()
    var maxLen = 0.0
    var maxSegment = Segment(list[0], list[1])
    var remain = list.size - 1
    while (remain != 0) {
        for (i in 0..remain) {
            if (list[remain].distance(list[i]) > maxLen) {
                maxLen = list[remain].distance(list[i])
                maxSegment = Segment(list[i], list[remain])
            }
        }
        remain--
    }
    return maxSegment
}

/**Проверка на правый поворот a -> b -> c (без аппроксимации) */
fun isNotRightTurn(a: Point, b: Point, c: Point): Boolean =
    (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x) >= 0

/**Проверка на правый поворот a -> b -> c (исключающая) */
fun isLeftTurn(a: Point, b: Point, c: Point): Boolean =
    (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x) > 0

/**Угол с осью OX */
fun ZeroVector.angleWithX(): Double = this.angleWith(ZeroVector(1.0, 0.0))

/**Алгоритм Грэхема с заданной точноостью*/
fun getHull(listInput: List<Point>, precision: Double): List<Point> {
    val p = listInput.minByOrNull { it.y }!!.let { Point(toZero(it.x, precision), toZero(it.y, precision)) }
    //в одну строчку на работает?
    val listFirstSort = listInput.map {
        Point(toZero(it.x, precision), toZero(it.y, precision))
    }
    val listSecondSort = listFirstSort.filter { abs(p.distance(it)) > delta }.sortedBy { p.distance(it) }

    val listThirdSort = listSecondSort.sortedBy { ZeroVector(p, it).angleWithX() }
    //тонкий момент. далее нужно убрать подряд идущие, и если ох угол через р о остью OX будет одинаковым,
    // то взять самое дальнее
    //sortedBy - stable, сохраняет порядок одинаковых элементов

    //удаление идущих под одним углом
    val list = mutableListOf<Point>()
    list.add(listThirdSort.first())
    for (i in 1..listThirdSort.size - 2) {
        if (abs(
                ZeroVector(p, listThirdSort[i]).angleWithX() - ZeroVector(p, listThirdSort[i + 1]).angleWithX()
            ) > delta
        ) {
            list.add(listThirdSort[i])
        }
    }
    list.add(listThirdSort.last())
    //сердце алгоритма
    val hull = Stack<Point>()
    hull.add(p)
    hull.add(list[0])
    for (index in 1 until list.size) {
        val pi = list[index]
        while (
            !isNotRightTurn(hull.previous(), hull.last(), pi)
        ) hull.pop()
        hull.push(pi)
    }
    //удаление крайне близких
    val hullAns = mutableListOf<Point>()
    var prev = hull[0]
    if (hull.first().distance(hull.last()) < delta) hull.pop()//для проверки перехода из последней к первой
    hullAns.add(hull[0])
    for (point in hull) {
        if (point.distance(prev) > precision) hullAns.add(point)
        prev = point
    }
    //удаление идущих в ряд
    var i = 1
    while (i < hullAns.size - 1) {
        if (!isLeftTurn(hullAns[i - 1], hullAns[i], hullAns[i + 1])) hullAns.removeAt(i) else i++
    }
    return hullAns.toList()
}

/**Отрезок, соединяющий две наиболее удалённые точки */
fun diameter(vararg points: Point): Segment { //diameterCustomTests() <- тесты тут
    if (points.size < 2) throw IllegalArgumentException()
    if (points.size == 2) return Segment(points[0], points[1])
    val hull: List<Point> //= listOf()
    try {
        hull = getHull(points.toList(), delta)
    } catch (e: Exception) {
        throw Exception(points.toList().toString())
    }
    if (hull.size < 2) throw Exception(points.toList().toString())
    var pointIndex = 0
    var oppositeIndex = hull.indices.maxByOrNull { i -> hull[i].y }!!

    //var sumAngle = 0.0

    var result = Segment(hull[0], hull[1])
    var max = result.length()
    var calipersAngle = 0.0

    fun checkMax(point: Point, opposite: Point) {
        val athwart = Segment(point, opposite)
        if (athwart.length() > max) {
            max = athwart.length()
            result = athwart
        }
    }

    while (pointIndex < hull.size + 1) { //движемся против часовой стрелки
        val point = hull.getFromPos(pointIndex)
        val opposite = hull.getFromPos(oppositeIndex)
        val nextPoint = hull.getFromPos(pointIndex + 1)
        val nextOpposite = hull.getFromPos(oppositeIndex + 1)
        val pointVectorMoveTo = ZeroVector(point, nextPoint)
        val oppositeVectorMoveTo = ZeroVector(opposite, nextOpposite)
        checkMax(point, opposite)
        var pointAngleMoveTo = pointVectorMoveTo.angleWith(ZeroVector(1.0, 0.0))
        if (pointVectorMoveTo.y < 0) pointAngleMoveTo = 2 * PI - pointAngleMoveTo
        var oppositeAngleMoveTo = oppositeVectorMoveTo.angleWith(ZeroVector(1.0, 0.0))
        if (oppositeVectorMoveTo.y < 0) oppositeAngleMoveTo = 2 * PI - oppositeAngleMoveTo
        val pointAngle = (PI * 2 + pointAngleMoveTo - calipersAngle) % PI
        val oppositeAngle = (PI * 2 + oppositeAngleMoveTo - (calipersAngle + PI) % (PI * 2)) % PI
        //println("${pointIndex % hull.size}\t${oppositeIndex % hull.size}\t${hull.size}")
        if (pointIndex % hull.size == oppositeIndex % hull.size) throw Exception(points.toList().toString())
        when {
            abs(pointAngle - oppositeAngle) < delta * delta -> {
                pointIndex++
                oppositeIndex++
                checkMax(nextPoint, opposite)
                checkMax(point, nextOpposite)
                calipersAngle += pointAngleMoveTo
            }
            pointAngle < oppositeAngle -> {
                pointIndex++
                calipersAngle = pointAngleMoveTo
            }
            pointAngle > oppositeAngle -> {
                oppositeIndex++
                calipersAngle = oppositeAngleMoveTo
            }
            else -> throw Exception(points.toList().toString())
        }
    }
    return result
}

fun main() {
    val list = listOf(
        Point(0.0, 0.0),
        Point(1.0, 4.0),
        Point(-2.0, 2.0),
        Point(3.0, -1.0),
        Point(-3.0, -2.0),
        Point(0.0, 5.0),
    )
    val diameter = diameter(*list.toTypedArray())
    val diameterOld = diameterOld(*list.toTypedArray())
    val hull = getHull(list, delta)
    println(diameter)
    println(hull.indexOf(diameter.begin))
    println(hull.indexOf(diameter.end))
    println(diameterOld)
    println(hull.indexOf(diameterOld.begin))
    println(hull.indexOf(diameterOld.end))
}