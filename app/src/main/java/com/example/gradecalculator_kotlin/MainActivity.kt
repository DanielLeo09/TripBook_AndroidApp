package com.example.gradecalculator_kotlin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gradecalculator_kotlin.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val calculator  = GradeCalculator()
    private val studentList = mutableListOf<Student>()
    private lateinit var adapter: StudentAdapter

    // File picker launcher
    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            handleImportedFile(uri)
        }
    }

    // File save launcher
    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            exportToExcel(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupClickListeners()
        refreshUI()
    }

    // RecyclerView
    private fun setupRecyclerView() {
        adapter = StudentAdapter(
            context       = this,
            students      = studentList,
            onDeleteClick = { position ->
                studentList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, studentList.size)
                refreshUI()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    // Button listeners
    private fun setupClickListeners() {

        binding.btnImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                ))
            }
            openFileLauncher.launch(intent)
        }

        binding.btnAdd.setOnClickListener { addStudentManually() }

        binding.btnCalculate.setOnClickListener { calculateGrades() }

        binding.btnExport.setOnClickListener {
            if (studentList.isEmpty()) { showToast("No students to export."); return@setOnClickListener }
            if (studentList.none { it.grade.isNotEmpty() }) { showToast("Calculate grades first."); return@setOnClickListener }
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_TITLE, "GradeReport.xlsx")
            }
            saveFileLauncher.launch(intent)
        }

        binding.btnClear.setOnClickListener {
            studentList.clear()
            adapter.notifyDataSetChanged()
            binding.tvFileName.visibility = View.GONE
            binding.cardStats.visibility  = View.GONE
            refreshUI()
            showToast("All students cleared.")
        }
    }

    // Manual entry
    private fun addStudentManually() {
        val name    = binding.etStudentName.text.toString().trim()
        val markStr = binding.etStudentMark.text.toString().trim()

        if (name.isEmpty()) { binding.tilName.error = "Enter a student name"; return }
        else binding.tilName.error = null

        val mark = markStr.toDoubleOrNull()
        if (mark == null)          { binding.tilMark.error = "Enter a valid number"; return }
        if (mark < 0 || mark > 100){ binding.tilMark.error = "Mark must be 0–100";   return }
        else binding.tilMark.error = null

        // Run grade calculation on IO thread, update UI on Main
        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                calculator.processStudents(listOf(Student(name = name, mark = mark)))
            }
            studentList.addAll(result)
            adapter.notifyItemInserted(studentList.size - 1)
            binding.etStudentName.text?.clear()
            binding.etStudentMark.text?.clear()
            binding.etStudentName.requestFocus()
            refreshUI()
            showToast("Added: ${result.first().name} — Grade: ${result.first().grade}")
        }
    }

    // Import Excel
    private fun handleImportedFile(uri: Uri) {
        showLoading(true)

        lifecycleScope.launch {
            // Dispatchers.IO — file reading happens here, NOT on main thread
            val readResult = withContext(Dispatchers.IO) {
                ExcelManager.readStudentsFromUri(this@MainActivity, uri)
            }

            // Back on main thread to update UI
            showLoading(false)
            readResult
                .onSuccess { imported ->
                    if (imported.isEmpty()) {
                        showToast("No valid data found. Check format: Column A = Name, Column B = Mark")
                    } else {
                        studentList.addAll(imported)
                        adapter.notifyDataSetChanged()
                        val fileName = getFileName(uri)
                        binding.tvFileName.text = "📄 $fileName  (${imported.size} students)"
                        binding.tvFileName.visibility = View.VISIBLE
                        refreshUI()
                        showToast("Imported ${imported.size} students.")
                    }
                }
                .onFailure { showToast("Import error: ${it.message}") }
        }
    }

    // Calculate grades
    private fun calculateGrades() {
        if (studentList.isEmpty()) { showToast("No students to calculate."); return }
        showLoading(true)

        lifecycleScope.launch {
            var count = 0
            val graded = withContext(Dispatchers.Default) {
                calculator.processStudents(studentList.toList()) { count++ }
            }

            // Back on main thread
            studentList.clear()
            studentList.addAll(graded)
            adapter.notifyDataSetChanged()
            showLoading(false)
            showStatsCard()
            refreshUI()
            showToast("Grades calculated for $count students.")
        }
    }

    // Export Excel_runs POI on IO thread
    private fun exportToExcel(uri: Uri) {
        showLoading(true)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        ExcelManager.writeStudentsToStream(studentList, out)
                    } ?: Result.failure(Exception("Could not open output location"))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            showLoading(false)
            result
                ?.onSuccess { showToast("✅ File saved successfully!") }
                ?.onFailure { showToast("Export error: ${it.message}") }
        }
    }

    // Stats card
    private fun showStatsCard() {
        val stats = calculator.getStats(studentList) ?: return
        binding.cardStats.visibility   = View.VISIBLE
        binding.tvStatAverage.text     = "Average Mark:  %.1f%%".format(stats.average)
        binding.tvStatHighest.text     = "Highest Mark:  %.1f%%".format(stats.highest)
        binding.tvStatLowest.text      = "Lowest Mark:   %.1f%%".format(stats.lowest)
        binding.tvStatPassing.text     = "Passing (≥50): ${stats.passing} / ${studentList.size}"
        binding.tvStatFailing.text     = "Failing (<50): ${stats.failing} / ${studentList.size}"
        val top = calculator.filterStudents(studentList) { it.grade.startsWith("A") }
        binding.tvStatTopStudents.text = "A-grade students: ${top.size}"
        binding.tvGradeBreakdown.text  =
            "A: ${stats.gradeACount}  B: ${stats.gradeBCount}  " +
                    "C: ${stats.gradeCCount}  D: ${stats.gradeDCount}  F: ${stats.gradeFCount}"
    }

    // UI helpers
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        // Disable buttons while loading so user can't double-tap
        binding.btnImport.isEnabled    = !show
        binding.btnAdd.isEnabled       = !show
        binding.btnCalculate.isEnabled = !show && studentList.isNotEmpty()
        binding.btnExport.isEnabled    = !show && studentList.any { it.grade.isNotEmpty() }
        binding.btnClear.isEnabled     = !show && studentList.isNotEmpty()
    }

    private fun refreshUI() {
        val hasStudents = studentList.isNotEmpty()
        val hasGraded   = studentList.any { it.grade.isNotEmpty() }
        binding.btnCalculate.isEnabled  = hasStudents
        binding.btnExport.isEnabled     = hasGraded
        binding.btnClear.isEnabled      = hasStudents
        binding.tvEmptyState.visibility = if (hasStudents) View.GONE  else View.VISIBLE
        binding.recyclerView.visibility = if (hasStudents) View.VISIBLE else View.GONE
        binding.tvStudentCount.text     = "Total students: ${studentList.size}"
    }

    private fun getFileName(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (idx >= 0) cursor.getString(idx) else "file.xlsx"
        } ?: "file.xlsx"
    }

    private fun showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}