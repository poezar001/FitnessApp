package com.example.fitnessapp.utils

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

object ChartUtils {

    fun setupBarChart(barChart: BarChart, labels: List<String>, values: List<Float>, color: Int) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "Workouts")
        dataSet.color = color
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barChart.data = barData

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.textColor = Color.WHITE

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.legend.textColor = Color.WHITE
        barChart.invalidate()
    }

    fun setupLineChart(lineChart: LineChart, labels: List<String>, values: List<Float>, color: Int) {
        val entries = values.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, "Calories")
        dataSet.color = color
        dataSet.setCircleColor(color)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE

        lineChart.description.isEnabled = false
        lineChart.legend.textColor = Color.WHITE
        lineChart.invalidate()
    }

    // For Activity Distribution Pie Chart
    fun setupPieChart(pieChart: PieChart, values: Map<String, Float>, colors: List<Int>) {
        val entries = values.map { PieEntry(it.value, it.key) }
        val dataSet = PieDataSet(entries, "Activities")
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.legend.textColor = Color.WHITE
        pieChart.invalidate()
    }

    // For Goal Progress Ring (NEW - The one you need for Home screen)
    fun setupPieChart(pieChart: PieChart, completedPercentage: Float, baseColor: Int) {
        // Cap at 100 to prevent negative values
        val safeCompleted = completedPercentage.coerceIn(0f, 100f)
        val remaining = 100f - safeCompleted

        // Split the chart into completed vs remaining slices
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(safeCompleted, ""))  // Completed portion
        entries.add(PieEntry(remaining, ""))      // Remaining portion

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(baseColor, Color.parseColor("#333333"))  // Subtle dark gray for remaining
        dataSet.setDrawValues(false) // Clean ring look without raw percentage numbers inside

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false // Hide the labels box entirely
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT) // Matches background
        pieChart.transparentCircleRadius = 0f
        pieChart.invalidate()
    }
}