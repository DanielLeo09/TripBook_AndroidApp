package com.example.gradecalculator_kotlin

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

/**
 * Singleton object for all Excel (xlsx) read/write operations.
 * Uses Apache POI library.
 */
object ExcelManager {


    fun readStudentsFromUri(context: Context, uri: Uri): Result<List<Student>> {
        return try {
            val students = mutableListOf<Student>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook: Workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)

                // Iterate from row 1 (skip header row 0)
                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue

                    val nameCell = row.getCell(0)
                    val markCell = row.getCell(1)

                    if (nameCell == null || markCell == null) continue

                    val name = when (nameCell.cellType) {
                        CellType.STRING  -> nameCell.stringCellValue.trim()
                        CellType.NUMERIC -> nameCell.numericCellValue.toInt().toString()
                        else -> continue
                    }

                    val mark = when (markCell.cellType) {
                        CellType.NUMERIC -> markCell.numericCellValue
                        CellType.STRING  -> markCell.stringCellValue.toDoubleOrNull() ?: continue
                        else -> continue
                    }

                    if (name.isEmpty()) continue
                    if (mark < 0 || mark > 100) continue

                    students.add(Student(name = name, mark = mark))
                }

                workbook.close()
            } ?: return Result.failure(Exception("Could not open file"))

            Result.success(students)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to read Excel file: ${e.message}"))
        }
    }

    /**
     * Writes a list of graded students to an OutputStream as a .xlsx file.
     * Output columns: Name | Mark | Grade | Remarks
     * Grade cells are colour-coded by grade band.
     */
    fun writeStudentsToStream(students: List<Student>, outputStream: OutputStream): Result<Unit> {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Student Grades")


            val gradeColor: (String) -> Short = { grade ->
                when {
                    grade == "A+" || grade == "A"  -> IndexedColors.BRIGHT_GREEN.index
                    grade == "A-" || grade == "B+" -> IndexedColors.LIGHT_GREEN.index
                    grade == "B"  || grade == "B-" -> IndexedColors.LIGHT_YELLOW.index
                    grade == "C+" || grade == "C"  -> IndexedColors.GOLD.index
                    grade == "C-" || grade == "D"  -> IndexedColors.LIGHT_ORANGE.index
                    else                           -> IndexedColors.ROSE.index
                }
            }

            val makeColorStyle: (Short, Boolean) -> XSSFCellStyle = { colorIndex, bold ->
                (workbook.createCellStyle() as XSSFCellStyle).apply {
                    fillForegroundColor = colorIndex
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                    borderBottom = BorderStyle.THIN
                    borderTop    = BorderStyle.THIN
                    borderLeft   = BorderStyle.THIN
                    borderRight  = BorderStyle.THIN
                    val font = workbook.createFont().apply {
                        this.bold = bold
                        fontHeightInPoints = 11
                    }
                    setFont(font)
                }
            }

            val headerStyle: XSSFCellStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                fillForegroundColor = IndexedColors.DARK_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                val font = workbook.createFont().apply {
                    bold = true
                    color = IndexedColors.WHITE.index
                    fontHeightInPoints = 12
                }
                setFont(font)
            }

            val headerRow = sheet.createRow(0)
            headerRow.heightInPoints = 22f
            listOf("Student Name", "Mark (/100)", "Grade", "Remarks").forEachIndexed { col, title ->
                headerRow.createCell(col).apply {
                    setCellValue(title)
                    cellStyle = headerStyle
                }
            }

            students.forEachIndexed { index, student ->
                val row = sheet.createRow(index + 1)
                row.heightInPoints = 20f

                val baseStyle: XSSFCellStyle = makeColorStyle(gradeColor(student.grade), false)

                // FIX: cloneStyleFrom needs XSSFCellStyle, not CellStyle
                val nameStyle: XSSFCellStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
                    cloneStyleFrom(baseStyle)
                    alignment = HorizontalAlignment.LEFT
                }

                row.createCell(0).apply { setCellValue(student.name);    cellStyle = nameStyle }
                row.createCell(1).apply { setCellValue(student.mark);    cellStyle = baseStyle }
                row.createCell(2).apply { setCellValue(student.grade);   cellStyle = makeColorStyle(gradeColor(student.grade), true) }
                row.createCell(3).apply { setCellValue(student.remarks); cellStyle = baseStyle }
            }

            (0..3).forEach { sheet.autoSizeColumn(it) }

            workbook.write(outputStream)
            workbook.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to write Excel file: ${e.message}"))
        }
    }
}