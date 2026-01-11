import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLOutput;
import java.util.*;
public class multithreadDownloader {

    public static void main(String[] args) throws Exception {
//        url of the file to download
        String fileURL = "https://example.com/file.zip";
        String outputFile = "file.zip"; // outfile name you want

//        get file size
        long fileSize = getFileSize(fileURL);
        System.out.println("File Size: "+ fileSize + "bytes");

//        Determin number of threads
        int processor = Runtime.getRuntime().availableProcessors();
        int threadCount = processor >1 ? processor:1;

        System.out.println("Using "+ threadCount + "thread(s)");

//        calculate chunk size per thread . how much thread will download
        long chunkSize = fileSize/threadCount;

//        Threads array
        Thread [] threads = new Thread[threadCount];

//        start thread
        for(int i =0;i<threadCount;i++){
            long start = i*chunkSize;
            long end = (i==threadCount-1) ?fileSize -1 :start + chunkSize-1;

            threads[i] = new Thread(new DownloadTask(fileURL, outputFile, start, end));
            threads[i].start();
        }
//        wait for all threads to finish
        for(Thread t : threads){
            t.join();
        }

        System.out.println("Download completed successfully");
    }
//    function to get file size
    private  static long getFileSize(String fileUrl) throws Exception{
        HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
        conn.setRequestMethod("HEAD");
        conn.connect();
        long size = conn.getContentLength();
        conn.disconnect();
        return size;
    }
}


