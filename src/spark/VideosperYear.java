package spark;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

public class VideosperYear {
	
  public static void main(String[] args) throws Exception {
    long timeElapsed = System.currentTimeMillis();
    System.out.println("Started Processing");
    SparkConf conf = new SparkConf()
    .setMaster("local")
    .setAppName("YouTubeDM");
    JavaSparkContext sc = new JavaSparkContext(conf);
    //Valid log levels include: ALL, DEBUG, ERROR, FATAL, INFO, OFF, TRACE, WARN
    sc.setLogLevel("ERROR");
    
    JavaRDD<String> mRDD = sc.textFile("data"); //directory where the files are
    
    JavaPairRDD<Integer,String> sortedRDD = mRDD
//    		.filter(line -> line.split("\t").length > 6)
    		.mapToPair(
    		line -> {
    			String[] lineArr = line.split("\t");
    			String datetime = lineArr[6];
    			String year = datetime.substring(0,4);
    			return new Tuple2<String, Integer>(year,1);
    			
    		})
        	.reduceByKey((x, y) -> x + y)
        	.mapToPair(item->item.swap())
    		.sortByKey(false);
    
    long count = sortedRDD.count();

    
    List<Tuple2<Integer, String>> topTenTuples = sortedRDD.take(10);
    JavaPairRDD<Integer,String> topTenRdd = sc.parallelizePairs(topTenTuples);
    
    String output_dir = "output/spark/VideosperYear";
    
	//remove output directory if already there
	FileSystem fs = FileSystem.get(sc.hadoopConfiguration());
	fs.delete(new Path(output_dir), true); // delete dir, true for recursive
	topTenRdd.saveAsTextFile(output_dir);
	timeElapsed = System.currentTimeMillis() - timeElapsed;
	System.out.println("Done.Time taken (in seconds): " + timeElapsed/1000f);
	System.out.println("Processed Records: " + count);
    
    sc.stop();
    sc.close();
    
  }
}