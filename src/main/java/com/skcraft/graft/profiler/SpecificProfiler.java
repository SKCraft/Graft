package com.skcraft.graft.profiler;

import com.google.common.io.Closer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpecificProfiler {

    private static final SpecificProfiler instance = new SpecificProfiler();

    private boolean enabled = false;
    private Map<Record, Record> data = new HashMap<>();

    public static SpecificProfiler getInstance() {
        return instance;
    }

    private SpecificProfiler() {
    }

    public void start() {
        enabled = true;
    }

    public void stopAndDump(File file) {
        Map<Record, Record> oldData = this.data;
        enabled = false;
        data = new HashMap<>();

        File parentDir = file.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }

        ReportBuilder builder = new ReportBuilder(oldData.values(), file);
        Thread thread = new Thread(builder, "SpecificProfiler Report Builder");
        thread.start();
    }

    public void stopAndDump(String path) {
        stopAndDump(new File(path));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void record(String className, String world, int x, int y, int z, long time) {
        Map<Record, Record> data = this.data;
        if (enabled) {
            Record record = new Record(className, world, x, y, z, time);
            Record found = data.get(record);
            if (found == null) {
                data.put(record, record);
                found = record;
            }

            found.increment(time);
        }
    }

    private static class Record {
        private final String className;
        private final String world;
        private final int x;
        private final int y;
        private final int z;
        private long time;
        private final int hashCode;

        private Record(String className, String world, int x, int y, int z, long time) {
            this.className = className;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
            this.hashCode = generateHashCode();
        }

        public void increment(long time) {
            this.time += time;
        }

        private int generateHashCode() {
            int result = className.hashCode();
            result = 31 * result + world.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Record record = (Record) o;

            if (x != record.x) return false;
            if (y != record.y) return false;
            if (z != record.z) return false;
            if (!className.equals(record.className)) return false;
            if (!world.equals(record.world)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private static class ReportBuilder implements Runnable {
        private static final Logger logger = Logger.getLogger(ReportBuilder.class.getCanonicalName());
        private final Collection<Record> records;
        private final File file;

        public ReportBuilder(Collection<Record> records, File file) {
            this.records = records;
            this.file = file;
        }

        private static String wrap(String str) {
            str = str.replace("\\", "\\\\");
            str = str.replace(",", "\\,");
            return "\"" + str + "\"";
        }

        @Override
        public void run() {
            logger.info("SpecificProfiler: Building report for " + records.size() + " records...");

            Closer closer = Closer.create();
            try {
                FileWriter writer = closer.register(new FileWriter(file));
                BufferedWriter bw = closer.register(new BufferedWriter(writer));
                PrintWriter pw = closer.register(new PrintWriter(bw));

                pw.println("World,X,Y,Z,Class,Time\r\n");

                for (Record record : records) {
                    pw.print(wrap(record.world));
                    pw.print(",");
                    pw.print(record.x);
                    pw.print(",");
                    pw.print(record.y);
                    pw.print(",");
                    pw.print(record.z);
                    pw.print(",");
                    pw.print(wrap(record.className));
                    pw.print(",");
                    pw.print(record.time);
                    pw.print("\r\n");
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to write SpecificProfiler report", e);
            } finally {
                try {
                    closer.close();
                } catch (IOException ignored) {
                }
            }

            logger.info("Report built");
        }
    }

}
