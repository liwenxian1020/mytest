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
public class PhoneFlowCaseCount extends Configured implements Tool {
    @Override
    public int run(String[] args) throws Exception {
        /**
         * 初始化一个Job
         */
        Job job = Job.getInstance(this.getConf(),"FlowCount");
        job.setJarByClass(PhoneFlowCaseCount.class);
        /**
         * 配置MapReduce job
         */
        //Input：负责整个程序的输入
        job.setInputFormatClass(TextInputFormat.class);//指定MapReduce程序从哪里读数据
        TextInputFormat.setInputPaths(job, new Path("file:///L:\\江宗海老师\\Day05_20190821_MapReduce案例及原理深入\\05_资料书籍\\流量统计\\data_flow.dat"));
        //Map：启动多个Map task来对数据进行处理
        job.setMapperClass(TestMap.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(FlowBean.class);
        //Shuffle：分组、排序、分区、规约
//        job.setGroupingComparatorClass(null);//自定义分组比较器
//        job.setSortComparatorClass(null);//自定义排序比较器
//        job.setPartitionerClass(HashPartitioner.class);//设定分区的方式
//        job.setCombinerClass(null);//设置规约的类
        //Reduce
        job.setReducerClass(TestReduce.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);
        job.setNumReduceTasks(1);//设置reduce的个数
        //outupt：默认将数据写入HDFS
        job.setOutputFormatClass(TextOutputFormat.class);//指定将结果保存到什么地方
        TextOutputFormat.setOutputPath(job,new Path("file:///C:\\output\\phone\\count1"));
        /**
         * 提交整个Job
         */
        return job.waitForCompletion(true) ? 0:-1;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int status = ToolRunner.run(conf,new PhoneFlowCaseCount(),args);
        System.exit(status);
    }

    public static class TestMap extends Mapper<LongWritable,Text,Text,FlowBean>{
        private Text outputKey=new Text();
        private FlowBean outputValue=new FlowBean();
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line=value.toString();
            String[] items =line.split("\t");
            String phone =items[1];
            int upPackage=Integer.valueOf(items[6]);
            int upFlow=Integer.valueOf(items[7]);
            int downPackage=Integer.valueOf(items[8]);
            int downFlow=Integer.valueOf(items[9]);

            this.outputKey.set(phone);
            this.outputValue.setDownPackage(downPackage);
            this.outputValue.setDownFlow(downFlow);
            this.outputValue.setUpFlow(upFlow);
            this.outputValue.setUpPackage(upPackage);

            context.write(this.outputKey,this.outputValue);
        }
    }


    public static class TestReduce extends Reducer<Text,FlowBean,Text,FlowBean>{
        private FlowBean outputValue=new FlowBean();
        @Override
        protected void reduce(Text key, Iterable<FlowBean> values, Context context) throws IOException, InterruptedException {
    int sumUpPackage=0;
    int sumDownPackage=0;
    int sumUpFlow=0;
    int sumDownFlow=0;
            for (FlowBean value : values) {
                sumUpPackage+=value.getUpPackage();
                sumDownPackage+=value.getDownPackage();
                sumUpFlow+=value.getUpFlow();
                sumDownFlow+=value.getDownFlow();
            }
            this.outputValue.setUpFlow(sumUpFlow);
            this.outputValue.setDownFlow(sumDownFlow);
            this.outputValue.setDownPackage(sumDownPackage);
            this.outputValue.setUpPackage(sumUpPackage);
            context.write(key,this.outputValue);
        }
    }

}
