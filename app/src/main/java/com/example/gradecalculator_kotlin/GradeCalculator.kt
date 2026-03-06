package com.example.gradecalculator_kotlin

interface GradeEvaluator {
    fun evaluate(mark: Double): String
    fun getGradePoints(grade: String): Double
    fun getRemarks(grade: String): String
}


data class Student(
    val name: String,
    val mark: Double,
    val grade: String = "",
    val remarks: String = ""
)


class GradeCalculator : GradeEvaluator {

    private val gradeFromMark: (Double) -> String = { mark ->
        when {
            mark < 0 || mark > 100 -> throw IllegalArgumentException("Mark must be between 0 and 100")
            mark >= 90 -> "A"
            mark >= 80 -> "B+"
            mark >= 65 -> "B"
            mark >= 50 -> "C"
            mark >= 35 -> "D"
            else       -> "F"
        }
    }


    override fun evaluate(mark: Double): String = gradeFromMark(mark)

    override fun getGradePoints(grade: String): Double = when (grade) {
        "A+" -> 4.0
        "A"  -> 4.0
        "A-" -> 3.7
        "B+" -> 3.3
        "B"  -> 3.0
        "B-" -> 2.7
        "C+" -> 2.3
        "C"  -> 2.0
        "C-" -> 1.7
        "D"  -> 1.0
        else -> 0.0
    }

    override fun getRemarks(grade: String): String = when (grade) {
        "A"  -> "Excellent"
        "B+" -> "Very Good"
        "B", -> "Good"
        "C"  -> "Average"
        else       -> "Fail"
    }

    fun processStudents(
        students: List<Student>,
        onEach: (Student) -> Unit = {}          // default empty lambda
    ): List<Student> {
        return students.map { student ->
            val grade   = gradeFromMark(student.mark)
            val remarks = getRemarks(grade)
            val graded  = student.copy(grade = grade, remarks = remarks)
            onEach(graded)                       // invoke the callback lambda
            graded
        }
    }

    fun filterStudents(
        students: List<Student>,
        predicate: (Student) -> Boolean
    ): List<Student> = students.filter(predicate)

    fun getStats(students: List<Student>): GradeStats? {
        if (students.isEmpty()) return null
        val marks = students.map { it.mark }
        return GradeStats(
            average   = marks.average(),
            highest   = marks.max(),
            lowest    = marks.min(),
            passing   = students.count { it.mark >= 50 },
            failing   = students.count { it.mark < 50 },
            gradeACount = students.count { it.grade.startsWith("A") },
            gradeBCount = students.count { it.grade.startsWith("B") },
            gradeCCount = students.count { it.grade.startsWith("C") },
            gradeDCount = students.count { it.grade == "D" },
            gradeFCount = students.count { it.grade == "F" }
        )
    }
}

data class GradeStats(
    val average: Double,
    val highest: Double,
    val lowest: Double,
    val passing: Int,
    val failing: Int,
    val gradeACount: Int,
    val gradeBCount: Int,
    val gradeCCount: Int,
    val gradeDCount: Int,
    val gradeFCount: Int
)