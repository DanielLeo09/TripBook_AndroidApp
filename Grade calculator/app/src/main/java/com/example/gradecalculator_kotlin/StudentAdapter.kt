package com.example.gradecalculator_kotlin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter to display a list of Student objects.
 * Uses a lambda (onDeleteClick) as a callback — higher-order function pattern.
 */
class StudentAdapter(
    private val context: Context,
    private val students: MutableList<Student>,
    private val onDeleteClick: (Int) -> Unit    // lambda callback for delete action
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIndex:   TextView = itemView.findViewById(R.id.tvIndex)
        val tvName:    TextView = itemView.findViewById(R.id.tvName)
        val tvMark:    TextView = itemView.findViewById(R.id.tvMark)
        val tvGrade:   TextView = itemView.findViewById(R.id.tvGrade)
        val tvRemarks: TextView = itemView.findViewById(R.id.tvRemarks)
        val btnDelete: View     = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.tvIndex.text   = "${position + 1}"
        holder.tvName.text    = student.name
        holder.tvMark.text    = String.format("%.1f", student.mark)
        holder.tvGrade.text   = if (student.grade.isEmpty()) "—" else student.grade
        holder.tvRemarks.text = if (student.remarks.isEmpty()) "Not calculated" else student.remarks

        // Colour-code the grade badge
        val bgColor = gradeBackgroundColor(student.grade)
        holder.tvGrade.backgroundTintList =
            ContextCompat.getColorStateList(context, bgColor)

        // Invoke the lambda when delete is clicked
        holder.btnDelete.setOnClickListener {
            onDeleteClick(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = students.size

    // Lambda-style helper to map grade → color resource
    private val gradeBackgroundColor: (String) -> Int = { grade ->
        when {
            grade.startsWith("A") -> R.color.gradeA
            grade.startsWith("B") -> R.color.gradeB
            grade.startsWith("C") -> R.color.gradeC
            grade == "D"          -> R.color.gradeD
            grade == "F"          -> R.color.gradeF
            else                  -> R.color.gradeNone
        }
    }

    fun updateStudents(newStudents: List<Student>) {
        students.clear()
        students.addAll(newStudents)
        notifyDataSetChanged()
    }
}