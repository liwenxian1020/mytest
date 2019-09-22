package cn.itcast.bigdata.hadoop.mapreduce.model;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * Created by Frank on 2019/8/19.
 */
public class MRModel extends Configured implements Tool {
    @Override
    public int run(String[] args) throws Exception {
        /**
         * 初始化一个Job
         */
        Job job = Job.getInstance(this.getConf(),"model");
        job.setJarByClass(MRModel.class);
        /**
         * 配置MapReduce job
         */
        //Input：负责整个程序的输入
        job.setInputFormatClass(TextInputFormat.class);//指定MapReduce程序从哪里读数据
        TextInputFormat.setInputPaths(job, new Path(""));
        //Map：启动多个Map task来对数据进行处理
        job.setMapperClass(null);
        job.setMapOutputKeyClass(null);
        job.setMapOutputValueClass(null);
        //Shuffle：分组、排序、分区、规约
        job.setGroupingComparatorClass(null);//自定义分组比较器
        job.setSortComparatorClass(null);//自定义排序比较器
        job.setPartitionerClass(HashPartitioner.class);//设定分区的方式
        job.setCombinerClass(null);//设置规约的类
        //Reduce
        job.setReducerClass(null);
        job.setOutputKeyClass(null);
        job.setOutputValueClass(null);
        job.setNumReduceTasks(1);//设置reduce的个数
        //outupt：默认将数据写入HDFS
        job.setOutputFormatClass(TextOutputFormat.class);//指定将结果保存到什么地方
        TextOutputFormat.setOutputPath(job,new Path(""));
        /**
         * 提交整个Job
         */
        return job.waitForCompletion(true) ? 0:-1;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int status = ToolRunner.run(conf,new MRModel(),args);
        System.exit(status);
    }

    public static class TestMap extends Mapper<LongWritable,Text,NullWritable,NullWritable>{
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            super.map(key, value, context);
        }
    }


    public static class TestReduce extends Reducer<NullWritable,NullWritable,NullWritable,NullWritable>{
        @Override
        protected void reduce(NullWritable key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
            super.reduce(key, values, context);
        }
    }

}
