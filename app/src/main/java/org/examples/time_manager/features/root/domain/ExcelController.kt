package org.examples.time_manager.features.root.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.examples.time_manager.features.root.DayModel
import org.examples.time_manager.features.root.presentation.utils.formatHoursFromSeconds
import java.io.IOException
import java.time.format.DateTimeFormatter

class ExcelController {
    fun createExcelFile(context: Context, uri: Uri, dayPerMonth: List<DayModel>) {
        Log.d("ExcelController", "Starting with an excel file")
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("SampleSheet")

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        val rightAlignedStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.RIGHT  // Align content to the right
        }

        for (i in dayPerMonth.indices) {
            val day = dayPerMonth.elementAt(i)
            val dataRow = sheet.createRow(i)
            dataRow.createCell(0).setCellValue(day.date.format(formatter))
            dataRow.createCell(1).apply { cellStyle = rightAlignedStyle }
                .setCellValue(formatHoursFromSeconds(day.time))
        }

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)  // Write workbook content to output stream
                workbook.close()
            }
        } catch (e: IOException) {
            Log.d("ExcelController", e.toString())
            e.printStackTrace()
        }
    }
}