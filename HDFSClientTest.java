package cn.itcast.bigdata.hadoop.hdfs;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * 实现Java API 操作HDFS
 * 1-实例化一个文件系统对象
 * 2-调用该对象的方法来实现操作
 * 3-关闭文件系统对象
 * Created by liwenxian on 2019/8/20
 */
public class HDFSClientTest {

    /**
     * 通过URL来实现IO操作HDFS【不推荐使用】
     */
    @Test
    public void getFileSystemURL() throws IOException {
// 1.获取一个文件系统实例
        //注册一个URL
        URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
        //定义一个输入流
        InputStream inputStream = new URL("hdfs://Hadoop02:8020/input/wc/wordcount.txt").openStream();
        //定义输出流
        FileOutputStream outputStream = new FileOutputStream(new File("c:\\wc1.txt"));
        //实现输入输出的拷贝
        IOUtils.copy(inputStream,outputStream);
        //关闭
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
    }

    /**
     * 获取HDFS的文件系统实例
     * @throws IOException
     */
    @Test
    public FileSystem getHDFS() throws IOException, URISyntaxException, InterruptedException {
        //实例化一个配置管理对象：管理整个Hadoop中所有的属性，每一个Hadoop都必须有该对象
        Configuration conf = new Configuration();
        //如何获取到实际的HDFS地址1-在resource添加core-site.xml   2-手动在conf中设置  3-通过URI注册
        conf.set("fs.defaultFS", "hdfs://Hadoop02");
        //获取一个HDFS的对象  1-获取一个hdfs的地址   2-获取一个配置文件管理对象   3-已什么用户身份去执行
        FileSystem fs1 = FileSystem.get(new URI("hdfs://Hadoop02:8020"), conf, "root");
//        FileSystem fs2 = FileSystem.get(new URI("hdfs://bigdata01.itcast.cn:8020"),conf);
//        FileSystem fs3 = FileSystem.newInstance(conf);
//        FileSystem fs4 = FileSystem.newInstance(new URI("hdfs://bigdata01.itcast.cn:8020"),conf,"root");
//        System.out.println(fs1.toString());
        return fs1;
    }



//2.遍历文件系统下所有的文件
    @Test
    public void getFiles() throws InterruptedException, IOException, URISyntaxException {
        //获取一个文件系统对象
        FileSystem fs = this.getHDFS();
        //执行操作：列举某个目录下的文件
        Path path = new Path("/");
/*        FileStatus[] fileStatuses = fs.listStatus(path);
        //遍历所有的文件或者目录
        for (FileStatus fileStatuse : fileStatuses) {
            if(fs.isDirectory(fileStatuse.getPath())){
                System.out.println(fileStatuse.getPath()+" 这是一个目录");
            }else
                System.out.println(fileStatuse.getPath()+" 这是一个文件");
        }*/
        //递归遍历目录下所有的文件
        RemoteIterator<LocatedFileStatus> locatedFileStatusRemoteIterator = fs.listFiles(path, true);
        while(locatedFileStatusRemoteIterator.hasNext()){
            LocatedFileStatus next = locatedFileStatusRemoteIterator.next();
            System.out.println(next.getPath());
        }
        //关闭对象
        fs.close();
    }




//3.在DHFS创建文件夹
    @Test
    public void createDir() throws InterruptedException, IOException, URISyntaxException {
        //获取对象
        FileSystem fs = this.getHDFS();
        //执行操作
        Path path = new Path("/bigdata15/hdfs");//实例化一个hdfs路径的对象
        //判断该路径是否已存在
        if(fs.exists(path)){
            //如果存在就删除
            fs.delete(path,true);
        }
        fs.mkdirs(path);

        //关闭
        fs.close();
    }



    //4.下载文件：从HDFS上下载到本地
    @Test
    public void getFileFromHDFS() throws InterruptedException, IOException, URISyntaxException {
        FileSystem fs = this.getHDFS();
        //执行的操作：下载一个文件到本地
        fs.copyToLocalFile(new Path("/input/wc/wordcount.txt"),new Path("file:///c:\\wc2"));
        fs.close();
    }



    @Test
    public void mergeFiletoHDFS() throws InterruptedException, IOException, URISyntaxException {
        //构建hdfs的输出流
        FileSystem fs = this.getHDFS();
        FSDataOutputStream fsDataOutputStream = fs.create(new Path("/bigdata15/hdfs/merge.txt"));
        //构建本地的输入流
        LocalFileSystem local = FileSystem.getLocal(new Configuration());
        FileStatus[] fileStatuses = local.listStatus(new Path("file:///c:\\output"));
        for (FileStatus fileStatuse : fileStatuses) {
            //取每一个文件，打开构建输入流
            FSDataInputStream open = local.open(fileStatuse.getPath());
            //复制给输出流
            IOUtils.copy(open,fsDataOutputStream);
            //关闭当前的输入流
            IOUtils.closeQuietly(open);
        }
        //关闭资源
        IOUtils.closeQuietly(fsDataOutputStream);
        local.close();
        fs.close();
    }

    //从本地向dhfs上传文件
    @Test
    public void putData() throws URISyntaxException, IOException {
        FileSystem fs=FileSystem.newInstance(new URI("hdfs://node01:8020"),
                new Configuration());
        fs.copyFromLocalFile(new Path("file:///c:\\install.log"),new
                Path("/hello/mydir/test"));
        fs.close();
    }
}
