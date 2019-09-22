package cn.itcast.bigdata.hadoop.mapreduce.phone;

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
public class PhoneFlowSort extends Configured implements Tool {
    @Override
    public int run(String[] args) throws Exception {
        /**
         * 初始化一个Job
         */
        Job job = Job.getInstance(this.getConf(),"sort");
        job.setJarByClass(PhoneFlowSort.class);
        /**
         * 配置MapReduce job
         */
        //Input：负责整个程序的输入
        job.setInputFormatClass(TextInputFormat.class);//指定MapReduce程序从哪里读数据
        TextInputFormat.setInputPaths(job, new Path("file:///c:\\output\\phone\\count1"));
        //Map：启动多个Map task来对数据进行处理
        job.setMapperClass(TestMap.class);
        job.setMapOutputKeyClass(FlowBean.class);
        job.setMapOutputValueClass(Text.class);
        //Shuffle：分组、排序、分区、规约
//        job.setGroupingComparatorClass(null);//自定义分组比较器
//        job.setSortComparatorClass(null);//自定义排序比较器，一般不用
//        job.setPartitionerClass(HashPartitioner.class);//设定分区的方式
//        job.setCombinerClass(null);//设置规约的类
        //Reduce
        job.setReducerClass(TestReduce.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);
        job.setNumReduceTasks(1);//设置reduce的个数
        //outupt：默认将数据写入HDFS
        job.setOutputFormatClass(TextOutputFormat.class);//指定将结果保存到什么地方
        TextOutputFormat.setOutputPath(job,new Path("file:///c:\\output\\phone\\sort1"));
        /**
         * 提交整个Job
         */
        return job.waitForCompletion(true) ? 0:-1;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int status = ToolRunner.run(conf,new PhoneFlowSort(),args);
        System.exit(status);
    }

    public static class TestMap extends Mapper<LongWritable,Text,FlowBean,Text>{

        private FlowBean outputKey = new FlowBean();
        private Text outputValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            //将文件中的每一行的五列读出来
            String[] items = value.toString().split("\t");
            String phone = items[0];
            int upPackage = Integer.valueOf(items[1]);
            int downPackage = Integer.valueOf(items[2]);
            int upFlow = Integer.valueOf(items[3]);
            int downFlow = Integer.valueOf(items[4]);
            //赋值
            this.outputKey.setUpPackage(upPackage);
            this.outputKey.setDownPackage(downPackage);
            this.outputKey.setUpFlow(upFlow);
            this.outputKey.setDownFlow(downFlow);
            this.outputValue.set(phone);
            //输出
            context.write(this.outputKey,this.outputValue);
        }
    }


    public static class TestReduce extends Reducer<FlowBean,Text,Text,FlowBean>{
        @Override
        protected void reduce(FlowBean key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                context.write(value,key);
            }
        }
    }

}
