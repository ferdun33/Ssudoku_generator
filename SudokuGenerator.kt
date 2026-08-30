// SudokuGenerator.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import java.io.File
import kotlin.random.Random

class SudokuGenerator {
    @Parameter(names = ["--generate"])
    private var generate: String? = null

    @Parameter(names = ["--solve"])
    private var solve: String? = null

    @Parameter(names = ["--export-json"])
    private var exportJson: String? = null

    @Parameter(names = ["--export-csv"])
    private var exportCsv: String? = null

    @Parameter(names = ["--export-txt"])
    private var exportTxt: String? = null

    @Parameter(names = ["--import"])
    private var importFile: String? = null

    @Parameter(names = ["--print"])
    private var print: Boolean = false

    private val grid = Array(9) { IntArray(9) }
    private val rand = Random

    private fun isValid(row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until 9) {
            if (grid[row][i] == num || grid[i][col] == num) return false
        }
        val startRow = (row / 3) * 3
        val startCol = (col / 3) * 3
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                if (grid[startRow + i][startCol + j] == num) return false
            }
        }
        return true
    }

    private fun solve(): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (grid[row][col] == 0) {
                    for (num in 1..9) {
                        if (isValid(row, col, num)) {
                            grid[row][col] = num
                            if (solve()) return true
                            grid[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun countSolutions(limit: Int): Int {
        var count = 0
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (grid[row][col] == 0) {
                    for (num in 1..9) {
                        if (isValid(row, col, num)) {
                            grid[row][col] = num
                            count += countSolutions(limit)
                            grid[row][col] = 0
                            if (count >= limit) return count
                        }
                    }
                    return count
                }
            }
        }
        return 1
    }

    private fun fillDiagonalBlocks() {
        for (block in 0 until 9 step 3) {
            val nums = (1..9).shuffled()
            var idx = 0
            for (i in block until block + 3) {
                for (j in block until block + 3) {
                    grid[i][j] = nums[idx++]
                }
            }
        }
    }

    private fun generate(difficulty: String): Array<IntArray> {
        fillDiagonalBlocks()
        solve()
        val solution = grid.map { it.clone() }.toTypedArray()
        val cellsToRemove = when (difficulty) {
            "easy" -> 30
            "medium" -> 40
            "hard" -> 50
            "expert" -> 55
            else -> 40
        }
        val positions = (0 until 9).flatMap { r -> (0 until 9).map { c -> r to c } }.shuffled()
        var removed = 0
        for ((r, c) in positions) {
            if (removed >= cellsToRemove) break
            val backup = grid[r][c]
            grid[r][c] = 0
            val copy = copyGrid()
            if (copy.countSolutions(2) == 1) {
                removed++
            } else {
                grid[r][c] = backup
            }
        }
        return solution
    }

    private fun copyGrid(): SudokuGenerator {
        val copy = SudokuGenerator()
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                copy.grid[i][j] = grid[i][j]
            }
        }
        return copy
    }

    private fun printGrid(color: Boolean) {
        val reset = if (color) "\u001B[0m" else ""
        val blue = if (color) "\u001B[34m" else ""
        val white = if (color) "\u001B[37m" else ""
        for (i in 0 until 9) {
            val line = StringBuilder()
            for (j in 0 until 9) {
                val val_ = grid[i][j]
                val ch = if (val_ == 0) "." else val_.toString()
                line.append(if (val_ == 0) blue else white).append(ch).append(reset).append(" ")
                if (j == 2 || j == 5) line.append("| ")
            }
            println(line)
            if (i == 2 || i == 5) println("------+-------+------")
        }
    }

    private fun exportJson(filename: String) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        File(filename).writeText(gson.toJson(grid))
    }

    private fun exportCsv(filename: String) {
        File(filename).printWriter().use { pw ->
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    if (j > 0) pw.print(",")
                    pw.print(grid[i][j])
                }
                pw.println()
            }
        }
    }

    private fun exportTxt(filename: String) {
        File(filename).printWriter().use { pw ->
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    if (j > 0) pw.print(" ")
                    pw.print(grid[i][j])
                }
                pw.println()
            }
        }
    }

    private fun load(filename: String) {
        val content = File(filename).readText()
        if (filename.endsWith(".json")) {
            val gson = GsonBuilder().create()
            val data = gson.fromJson(content, Array<IntArray>::class.java)
            for (i in 0 until minOf(9, data.size)) {
                for (j in 0 until minOf(9, data[i].size)) {
                    grid[i][j] = data[i][j]
                }
            }
        } else {
            val lines = content.split("\n")
            for (i in 0 until minOf(9, lines.size)) {
                val parts = lines[i].trim().split(Regex("\\s+|,"))
                for (j in 0 until minOf(9, parts.size)) {
                    grid[i][j] = parts[j].toIntOrNull() ?: 0
                }
            }
        }
    }

    fun run() {
        when {
            generate != null -> {
                val solution = generate(generate!!)
                if (print) {
                    println("Generated $generate puzzle:")
                    printGrid(true)
                }
                exportJson?.let {
                    val data = mapOf("puzzle" to grid, "solution" to solution)
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    File(it).writeText(gson.toJson(data))
                    println("Exported to $it")
                }
                exportCsv?.let { exportCsv(it) }
                exportTxt?.let { exportTxt(it) }
            }
            solve != null -> {
                load(solve!!)
                if (solve()) {
                    if (print) {
                        println("Solved puzzle:")
                        printGrid(true)
                    }
                    exportJson?.let {
                        exportJson(it)
                        println("Exported to $it")
                    }
                } else {
                    println("No solution found.")
                }
            }
            importFile != null -> {
                load(importFile!!)
                if (print) printGrid(true)
            }
            else -> println("Use --help for usage.")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val gen = SudokuGenerator()
            JCommander.newBuilder().addObject(gen).build().parse(*args)
            gen.run()
        }
    }
}
