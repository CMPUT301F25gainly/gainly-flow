package com.example.gainly_flow;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class that exports a list of entrants for an event to a CSV file.
 */
public class CSVExporter {

    /**
     * Export the given entrants to a CSV file.
     *
     * The file is written to the app-specific Download directory:
     *   /Android/data/<package>/files/Download
     *
     * @param context   Activity or Application context
     * @param eventName Name of the event (used in filename)
     * @param entrants  Entrants to export
     * @return          The File that was created, or null on error
     */
    public File exportEntrants(Context context, String eventName, List<Entrant> entrants) {
        if (context == null || entrants == null || entrants.isEmpty()) {
            return null;
        }

        // Make the event name safe for use in a filename
        String safeEventName = "event";
        if (!TextUtils.isEmpty(eventName)) {
            safeEventName = eventName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        String fileName = safeEventName + "_entrants_" + timestamp + ".csv";

        // App-specific "Downloads" directory – no runtime storage permission needed
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = context.getFilesDir();
        }

        File csvFile = new File(dir, fileName);
        FileWriter writer = null;

        try {
            writer = new FileWriter(csvFile);
            // Header
            writer.append("id,name,email\n");

            for (Entrant e : entrants) {
                if (e == null) continue;

                String id = safe(e.getId());
                String name = safe(e.getDisplayName());
                String email = safe(e.getEmail());

                writer.append(id).append(',')
                        .append(name).append(',')
                        .append(email).append('\n');
            }

            writer.flush();
            return csvFile;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
            }
        }
    }

    /** Escape quotes and wrap the value to be CSV-safe. */
    private String safe(String s) {
        if (s == null) return "\"\"";
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
