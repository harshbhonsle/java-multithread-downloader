import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultithreadDownloader {

    public static void main(String[] args) throws Exception {
        // URL of the file to download
        String fileURL = "http://speedtest.tele2.net/10MB.zip";
        String outputFile = "fileTesting.zip"; // output file name

        // Get file size
        long fileSize = getFileSize(fileURL);
        System.out.println("File Size: " + fileSize + " bytes");

        // Fallback if file size is invalid
        if (fileSize <= 0) {
            System.out.println("⚠️ Falling back to single-thread download...");
            Thread t = new Thread(new DownloadTask(fileURL, outputFile, 0, Long.MAX_VALUE));
            t.start();
            t.join();
            System.out.println("Download completed (single-thread fallback).");
            return;
        }

        // Determine number of threads
        int processor = Runtime.getRuntime().availableProcessors();
        int threadCount = processor > 1 ? processor : 1;
        System.out.println("Using " + threadCount + " thread(s)");

        // Calculate chunk size per thread
        long chunkSize = fileSize / threadCount;

        // Threads array
        Thread[] threads = new Thread[threadCount];

        // Start threads
        for (int i = 0; i < threadCount; i++) {
            long start = i * chunkSize;
            long end = (i == threadCount - 1) ? fileSize - 1 : start + chunkSize - 1;

            threads[i] = new Thread(new DownloadTask(fileURL, outputFile, start, end));
            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread t : threads) {
            t.join();
        }

        System.out.println("Download completed successfully");
    }

    // Function to get file size
    private static long getFileSize(String fileUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
        conn.setRequestMethod("HEAD");
        conn.connect();

        String acceptRanges = conn.getHeaderField("Accept-Ranges");
        if (!"bytes".equalsIgnoreCase(acceptRanges)) {
            System.out.println("Server does not support multi-threaded download.");
            conn.disconnect();
            return -1; // signal fallback
        }

        long size = conn.getContentLengthLong();
        conn.disconnect();
        return size;
    }
}

class DownloadTask implements Runnable {
    private final String url;
    private final String fileName;
    private final long start;
    private final long end;

    DownloadTask(String url, String fileName, long start, long end) {
        this.url = url;
        this.fileName = fileName;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        int attempts = 0; // retry counter
        while (attempts < 3) { // allow 3 retry attempts
            try {
//                http connection to file url
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                // telling server which file range we want (for multi-threaded download) ex we want from 0-1023
                if (end != Long.MAX_VALUE) {
                    conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
                }
//                get in input stream to read data from server
                InputStream in = conn.getInputStream();
//                open the output file for read / write permission
                RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
//                move the file pointer to the correct position at the starting point (start)
                raf.seek(start);
//                this is the buffer where we are storing the chunks while downloading
                byte[] buffer = new byte[4096];
                int bytesRead;
//                Reading from server and writing into the file
                while ((bytesRead = in.read(buffer)) != -1) {
                    raf.write(buffer, 0, bytesRead);
                }
//                closing the resources
                raf.close();
                in.close();
                conn.disconnect();

                System.out.println(Thread.currentThread().getName() +
                        " finished downloading bytes " + start + " to " + end);
                break; // success, exit retry loop

            } catch (Exception e) {
                attempts++;
                System.out.println("Retrying chunk " + start + "-" + end +
                        " (attempt " + attempts + ")");
                if (attempts == 3) e.printStackTrace();
            }
        }
    }
}
