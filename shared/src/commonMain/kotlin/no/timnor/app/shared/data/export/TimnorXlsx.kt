package no.timnor.app.shared.data.export

import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.calendar.workSecondsByDay
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds

/** Creates the existing workbook contract: a Timer sheet with date and decimal-hour columns. */
fun createTimnorXlsx(works: List<StoredWork>, year: Int, month: Int): ByteArray {
    val secondsByDay = workSecondsByDay(
        entries = works,
        year = year,
        month = month,
        dateOf = { utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).date },
        secondsOf = StoredWork::time,
    )
    val rows = secondsByDay.toSortedMap().map { (day, seconds) ->
        val dateText = "${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.$year"
        dateText to (seconds / 3600.0).toString()
    }
    val sheetRows = rows.mapIndexed { index, (date, hours) ->
        val row = index + 1
        "<row r=\"$row\"><c r=\"A$row\" t=\"inlineStr\"><is><t>${xmlEscape(date)}</t></is></c>" +
            "<c r=\"B$row\" s=\"1\"><v>$hours</v></c></row>"
    }.joinToString("")
    val entries = listOf(
        "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""",
        "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
        "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Timer" sheetId="1" r:id="rId1"/></sheets></workbook>""",
        "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""",
        "xl/styles.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment horizontal="right"/></xf></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>""",
        "xl/worksheets/sheet1.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$sheetRows</sheetData></worksheet>""",
    ).map { (name, contents) -> name to contents.encodeToByteArray() }
    return storedZip(entries)
}

private fun xmlEscape(value: String): String = buildString(value.length) {
    value.forEach { character ->
        append(
            when (character) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&apos;"
                else -> character.toString()
            },
        )
    }
}

private data class ZipEntryMeta(
    val name: ByteArray,
    val crc: Long,
    val size: Long,
    val offset: Long,
)

/** Writes a small standards-compliant, uncompressed ZIP package for the OpenXML parts. */
private fun storedZip(entries: List<Pair<String, ByteArray>>): ByteArray {
    val output = mutableListOf<Byte>()
    val centralEntries = mutableListOf<ZipEntryMeta>()
    for ((nameText, contents) in entries) {
        val name = nameText.encodeToByteArray()
        val crc = crc32(contents)
        val offset = output.size.toLong()
        appendU32(output, 0x04034b50)
        appendU16(output, 20)
        appendU16(output, 0x0800)
        appendU16(output, 0)
        appendU16(output, 0)
        appendU16(output, 0x0021)
        appendU32(output, crc)
        appendU32(output, contents.size.toLong())
        appendU32(output, contents.size.toLong())
        appendU16(output, name.size)
        appendU16(output, 0)
        appendBytes(output, name)
        appendBytes(output, contents)
        centralEntries += ZipEntryMeta(name, crc, contents.size.toLong(), offset)
    }
    val centralOffset = output.size.toLong()
    centralEntries.forEach { entry ->
        appendU32(output, 0x02014b50)
        appendU16(output, 20)
        appendU16(output, 20)
        appendU16(output, 0x0800)
        appendU16(output, 0)
        appendU16(output, 0)
        appendU16(output, 0x0021)
        appendU32(output, entry.crc)
        appendU32(output, entry.size)
        appendU32(output, entry.size)
        appendU16(output, entry.name.size)
        appendU16(output, 0)
        appendU16(output, 0)
        appendU16(output, 0)
        appendU16(output, 0)
        appendU32(output, 0)
        appendU32(output, entry.offset)
        appendBytes(output, entry.name)
    }
    val centralSize = output.size - centralOffset
    appendU32(output, 0x06054b50)
    appendU16(output, 0)
    appendU16(output, 0)
    appendU16(output, centralEntries.size)
    appendU16(output, centralEntries.size)
    appendU32(output, centralSize)
    appendU32(output, centralOffset)
    appendU16(output, 0)
    return ByteArray(output.size) { index -> output[index] }
}

private fun appendU16(output: MutableList<Byte>, value: Int) {
    output += (value and 0xff).toByte()
    output += ((value ushr 8) and 0xff).toByte()
}

private fun appendU32(output: MutableList<Byte>, value: Long) {
    repeat(4) { index -> output += ((value ushr (index * 8)) and 0xff).toByte() }
}

private fun appendBytes(output: MutableList<Byte>, bytes: ByteArray) {
    bytes.forEach(output::add)
}

private fun crc32(bytes: ByteArray): Long {
    var crc = 0xffffffffL
    bytes.forEach { byte ->
        crc = crc xor (byte.toLong() and 0xff)
        repeat(8) {
            crc = if ((crc and 1L) != 0L) (crc ushr 1) xor 0xedb88320L else crc ushr 1
        }
    }
    return crc xor 0xffffffffL
}
