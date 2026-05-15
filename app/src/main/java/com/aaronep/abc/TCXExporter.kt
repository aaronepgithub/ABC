package com.aaronep.abc

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object TCXExporter {

    fun export(context: Context, points: List<TrackPoint>) {
        if (points.isEmpty()) {
            Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val filename = "ride_${System.currentTimeMillis()}.tcx"
        val content = StringBuilder()

        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        content.append("<TrainingCenterDatabase xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\">\n")
        content.append("  <Activities>\n")
        content.append("    <Activity Sport=\"Biking\">\n")
        content.append("      <Id>${dateFormat.format(Date(points.first().time))}</Id>\n")
        content.append("      <Lap StartTime=\"${dateFormat.format(Date(points.first().time))}\">\n")
        content.append("        <Track>\n")

        for (point in points) {
            content.append("          <Trackpoint>\n")
            content.append("            <Time>${dateFormat.format(Date(point.time))}</Time>\n")
            content.append("            <Position>\n")
            content.append("              <LatitudeDegrees>${point.latitude}</LatitudeDegrees>\n")
            content.append("              <LongitudeDegrees>${point.longitude}</LongitudeDegrees>\n")
            content.append("            </Position>\n")
            content.append("            <AltitudeMeters>${point.altitude}</AltitudeMeters>\n")
            content.append("            <Extensions>\n")
            content.append("              <TPX xmlns=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2\">\n")
            content.append("                <Watts>${point.power.toInt()}</Watts>\n")
            content.append("              </TPX>\n")
            content.append("            </Extensions>\n")
            content.append("          </Trackpoint>\n")
        }

        content.append("        </Track>\n")
        content.append("      </Lap>\n")
        content.append("    </Activity>\n")
        content.append("  </Activities>\n")
        content.append("</TrainingCenterDatabase>")

        saveFile(context, filename, content.toString())
    }

    private fun saveFile(context: Context, filename: String, content: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.garmin.tcx+xml")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            }
            Toast.makeText(context, "Saved to Downloads: $filename", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    }
}
