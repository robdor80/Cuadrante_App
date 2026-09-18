package es.robertodorado.cuadrante.model

/**
 * An immutable, non-empty sequence of shifts that repeats indefinitely.
 */
class ShiftPattern(shifts: List<ShiftType>) {
    val shifts: List<ShiftType> = shifts.toList()

    init {
        require(this.shifts.isNotEmpty()) { "A shift pattern cannot be empty" }
    }

    val size: Int
        get() = shifts.size

    internal operator fun get(index: Int): ShiftType = shifts[index]
}
