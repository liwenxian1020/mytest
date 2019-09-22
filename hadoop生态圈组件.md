# flume

## 1.概述

Flume是Cloudera提供的一个高可用的，高可靠的，分布式的海量日志采集、聚合和传输的软件。

Flume的核心是把数据从数据源(source)收集过来，再将收集到的数据送到指定的目的地(sink)。为了保证输送的过程一定成功，在送到目的地(sink)之前，会先缓存数据(channel),待数据真正到达目的地(sink)后，flume在删除自己缓存的数据。

## 2.运行机制

![1567177209645](../%E6%AD%A6%E5%8A%9F%E7%A7%98%E7%B1%8D/flume.assets/1567177209645.png)

**source**：采集源，用于和数据源对接获得数据

**channel**：agent内部的数据通道，用于从source传输数据到silk

**silk**：下沉地，采集数据的传送目的，用于往下一级agent传递数据或者往。最终存储系统传递数据；

**flume的数据单元**：

整个数据传输过程中，流动的是event，是内部传输的最基本单元

```
Event: { headers:{} body: 68 65 6C 6C 6F 0D      hello. }
```

## 3.Flume采集系统结构图

### 3.1简单结构

单个agent采集数据

![1567177616159](../%E6%AD%A6%E5%8A%9F%E7%A7%98%E7%B1%8D/flume.assets/1567177616159.png)

### 3.2复杂结构

多级agent之间串联

![1567177634219](../%E6%AD%A6%E5%8A%9F%E7%A7%98%E7%B1%8D/flume.assets/1567177634219.png)



## 4.flume安装

解压安装文件，修改conf目录下的flume-evn.sh文件，在里面配置java_home

测试：

![1567251725316](../%E6%AD%A6%E5%8A%9F%E7%A7%98%E7%B1%8D/flume.assets/1567251725316.png)

1、在flume的conf目录下新建一个文件

vi netcat-logger.conf

```properties
# 定义这个agent中各组件的名字
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# 描述和配置source组件：r1
a1.sources.r1.type = netcat
a1.sources.r1.bind = localhost
a1.sources.r1.port = 44444

# 描述和配置sink组件：k1
a1.sinks.k1.type = logger

# 描述和配置channel组件，此处使用是内存缓存的方式
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# 描述和配置source  channel   sink之间的连接关系
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

2、启动agent去采集数据

```shell
bin/flume-ng agent --conf conf --conf-file conf/netcat-logger.conf --name a1 -Dflume.root.logger=INFO,console  全写版本

bin/flume-ng agent -c ./conf -f ./conf/spool-hdfs.conf -n a1 -Dflume.root.logger=INFO,console  简写版本

--conf（-c）  指定配置文件夹路径
--conf-file（-f） 指定采集方案是哪一个文件
--name（-n）  agent的名字 和采集方案里面的名字保持一致
-Dflume.root.logger=INFO,console  开启日志 把日志输出在console终端
```

-c conf   指定flume自身的配置文件所在目录

-f conf/netcat-logger.conf  指定我们所描述的采集方案

-n a1  指定我们这个agent的名字

3、测试

先要往agent采集监听的端口上发送数据，让agent有数据可采。

随便在一个能跟agent节点联网的机器上：

```shell
#telnet anget-hostname  port   
telnet localhost 44444
```



## 5.flume案例

### 5.1、采集目录到HDFS(spooldir)

业务需求：服务器的某特定目录下，会不断产生新的文件，每当有新文件出现，就需要把文件采集到HDFS中去。

source: 监控文件目录  spooldir

channle: file channel | memory

sink: HDFS文件系统  hdfs sink

配置文件：

```properties
# Name the components on this agent
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# Describe/configure the source
##注意：不能往监控目中重复丢同名文件
a1.sources.r1.type = spooldir
a1.sources.r1.spoolDir = /root/logs2
a1.sources.r1.fileHeader = true

# Describe the sink
a1.sinks.k1.type = hdfs
a1.sinks.k1.channel = c1
a1.sinks.k1.hdfs.path = /flume/events/%y-%m-%d/%H%M/
#文件前缀
a1.sinks.k1.hdfs.filePrefix = events-
#round=false 是否启用时间上的舍弃，类似于四舍五入
#roundValue=1 每隔多久滚动文件夹
#roundUnit=second 时间单位
a1.sinks.k1.hdfs.round = true
a1.sinks.k1.hdfs.roundValue = 10
a1.sinks.k1.hdfs.roundUnit = minute
#rollInterval=30s 间隔多长时间将临时文件滚动成最终目标文件
#rollSize=1024b 当临时文件达到该大小滚动成目标文件
#rollCount=10 当events数据达到该数量滚动文件
a1.sinks.k1.hdfs.rollInterval = 3
a1.sinks.k1.hdfs.rollSize = 20
a1.sinks.k1.hdfs.rollCount = 5
a1.sinks.k1.hdfs.batchSize = 1
a1.sinks.k1.hdfs.useLocalTimeStamp = true
#生成的文件类型，默认是Sequencefile，可用DataStream，则为普通文本
a1.sinks.k1.hdfs.fileType = DataStream

# Use a channel which buffers events in memory
a1.channels.c1.type = memory
#该通道中最大的可以存储的event数量
a1.channels.c1.capacity = 1000
#每次最大可以从source中拿到或者送到sink中的event数量
a1.channels.c1.transactionCapacity = 100

# Bind the source and sink to the channel
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

启动命令：

```shell
bin/flume-ng agent -c ./conf -f ./conf/spool-hdfs.conf -n a1 -Dflume.root.logger=INFO,console
```

### 5.2、采集文件到HDFS(exec）

业务需求：比如业务系统使用log4j生成的日志，日志内容不断增加，需要把追加到日志文件中的数据实时采集到hdfs

source: 监控文件内容更新   exec  'tail -F file'

channel：可用lfile channel 也可以用 内存channel

sink：hdfs sink

```properties
# Name the components on this agent
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# Describe/configure the source
a1.sources.r1.type = exec
a1.sources.r1.command = tail -F /root/logs/test.log
a1.sources.r1.channels = c1

# Describe the sink
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path = /flume/tailout/%y-%m-%d/%H%M/
a1.sinks.k1.hdfs.filePrefix = events-
a1.sinks.k1.hdfs.round = true
a1.sinks.k1.hdfs.roundValue = 10
a1.sinks.k1.hdfs.roundUnit = minute
a1.sinks.k1.hdfs.rollInterval = 3
a1.sinks.k1.hdfs.rollSize = 20
a1.sinks.k1.hdfs.rollCount = 5
a1.sinks.k1.hdfs.batchSize = 1
a1.sinks.k1.hdfs.useLocalTimeStamp = true
#生成的文件类型，默认是Sequencefile，可用DataStream，则为普通文本
a1.sinks.k1.hdfs.fileType = DataStream

# Use a channel which buffers events in memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind the source and sink to the channel
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

启动命令：

```shell
bin/flume-ng agent -c ./conf -f ./conf/exec-hdfs.conf -n a1 -Dflume.root.logger=INFO,console
```



## 6.Flume高阶特性

### 6.1 load-balance负载均衡

- 用于解决一个进程或者服务处理不了所有请求 多个进程或者服务一起处理的场景。
- 负载均衡涉及算法：轮询（round_robin）  随机（random）  权重（当前flume不支持）
- 注意：数据或者请求只能发送给一个 避免数据重复 请求重复的问题
- 注意：当数据量小的时候 一个机器或者进程可以解决完所有请求 此时负载均衡是失效的。

![1567253717742](../%E6%AD%A6%E5%8A%9F%E7%A7%98%E7%B1%8D/flume.assets/1567253717742.png)

```properties
a1.sinkgroups = g1
a1.sinkgroups.g1.sinks = k1 k2 k3
a1.sinkgroups.g1.processor.type = load_balance
#如果开启，则将失败的sink放入黑名单
a1.sinkgroups.g1.processor.backoff = true  
# 另外还支持random
a1.sinkgroups.g1.processor.selector = round_robin  
#在黑名单放置的超时时间，超时结束时，若仍然无法接收，则超时时间呈指数增长
a1.sinkgroups.g1.processor.selector.maxTimeOut=10000 
```

### 6.2 failover容错

- 用于解决单点故障问题 为单点故障设备备份
- 常见的是一主一备
- 当主出现故障 备份的才切换成为主 继续提供服务 保证业务的连续性   形成所谓的高可用（HA）。
- 注意：数据只能发送给主 所谓的活跃的（active）

```properties
a1.sinkgroups = g1
a1.sinkgroups.g1.sinks = k1 k2 k3
a1.sinkgroups.g1.processor.type = failover
#优先级值, 绝对值越大表示优先级越高
a1.sinkgroups.g1.processor.priority.k1 = 5 
a1.sinkgroups.g1.processor.priority.k2 = 7
a1.sinkgroups.g1.processor.priority.k3 = 6
#失败的Sink回切时间（millis）
a1.sinkgroups.g1.processor.maxpenalty = 2000  
```

## 7.Flume拦截器

拦截器（interceptor）是Flume中简单的插件式组件，设置在source和channel之间。source接收到的event事件，在写入channel之前，拦截器都可以进行转换或者删除这些事件。每个拦截器只处理同一个source接收到的事件。

### 7.1 timestamp interceptor

该拦截器的作用是将时间戳插入到flume的事件header中。如果不使用任何拦截器，flume接受到的只有message。

```properties
a1.sources.r1.interceptors = timestamp 
a1.sources.r1.interceptors.timestamp.type=timestamp 
#如果设置为true，若事件中报头已经存在，不会替换时间戳报头的值。
a1.sources.r1.interceptors.timestamp.preserveExisting=false
```

### 7.2 host interceptor

主机拦截器插入服务器的ip地址或者主机名，agent将这些内容插入到事件的报头中。

```properties
a1.sources.r1.interceptors = host 
a1.sources.r1.interceptors.host.type=host 
a1.sources.r1.interceptors.host.useIP=false 
a1.sources.r1.interceptors.timestamp.preserveExisting=true
```

### 7.3 时间拦截器案例

需求描述：

![1567254735771](../%E6%AD%A6%E5%8A%9F%E7%A7%98%E7%B1%8D/flume.assets/1567254735771.png)

配置文件：

```properties
# specify agent,source,sink,channel
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# handler将根据JSON规则，提取出header、body，然后生成flume event的header、body
a1.sources.r1.type = http
a1.sources.r1.bind = node-1
a1.sources.r1.port = 8888
a1.sources.r1.handler = org.apache.flume.source.http.JSONHandler

# interceptor将在flume event的header中增加时间戳
# 该interceptor将在flume event的header中增加当前系统时间
a1.sources.r1.interceptors = i1  
a1.sources.r1.interceptors.i1.type = timestamp
a1.sources.r1.interceptors.i1.preserveExisting= false  

# hdfs sink
a1.sinks.k1.type = hdfs   
# sink将会基于flume event头部的时间戳来提取年月日信息，在HFDS上创建目录
a1.sinks.k1.hdfs.path = hdfs://node-1:8020/flume/%Y-%m-%d/

# 如果event的header中没有时间戳，就要打开下面的配置
# a1.sinks.k1.hdfs.useLocalTimeStamp = true

a1.sinks.k1.hdfs.filePrefix = interceptor-
a1.sinks.k1.hdfs.fileType=DataStream
a1.sinks.k1.hdfs.wirteFormat = Text
a1.sinks.k1.hdfs.rollSize = 102400000
a1.sinks.k1.hdfs.rollCount = 5
a1.sinks.k1.hdfs.rollInterval = 3

# channel, memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# bind source,sink to channel
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

测试：

```shell
curl -X POST -d '[{"headers":{}, "body":"timestamp teset 001"}]'  http://hdp1:8888
```

-X POST 表示使用HTTP POST方法，将 -d 指明的 json格式的数据，发送给node-1的8888端口



### 7.4 时间+主机名拦截器

需求描述：

![1567254810755](../%E6%AD%A6%E5%8A%9F%E7%A7%98%E7%B1%8D/flume.assets/1567254810755.png)

配置文件：

```properties
# 01 define agent name, source/sink/channel name
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# 02 source,http,jsonhandler
a1.sources.r1.type = http
a1.sources.r1.bind = node-1
a1.sources.r1.port = 8888
a1.sources.r1.handler = org.apache.flume.source.http.JSONHandler

# 03 timestamp and host interceptors work before source
a1.sources.r1.interceptors = i1 i2       # 两个interceptor串联，依次作用于event
a1.sources.r1.interceptors.i1.type = timestamp 
a1.sources.r1.interceptors.i1.preserveExisting = false  
 
a1.sources.r1.interceptors.i2.type = host  
# flume event的头部将添加 “hostname”:实际主机名
a1.sources.r1.interceptors.i2.hostHeader = hostname  # 指定key,value将填充为flume agent所在节点的主机名
a1.sources.r1.interceptors.i2.useIP = false          # IP和主机名，二选一即可

# 04 hdfs sink
a1.sinks.k1.type = hdfs  
a1.sinks.k1.hdfs.path = hdfs://node-1:8020/flume/%Y-%m-%d/   # hdfs sink将根据event header中的时间戳进行替换
# 和hostHeader的值保持一致，hdfs sink将提取event中key为hostnmae的值，基于该值创建文件名前缀
a1.sinks.k1.hdfs.filePrefix = %{hostname}   # hdfs sink将根据event header中的hostnmae对应的value进行替换
a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
a1.sinks.k1.hdfs.rollInterval = 0
a1.sinks.k1.hdfs.rollCount = 3
a1.sinks.k1.hdfs.rollSize = 1024000

# channel,memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# bind source,sink to channel 
a1.sinks.k1.channel = c1
a1.sources.r1.channels = c1
```

### 7.5 static interceptor 案例

需求：把A、B 机器中的access.log、nginx.log、web.log 采集汇总到C机器上然后统一收集到hdfs中。但是在hdfs中要求的目录为：

/source/logs/access/20160101/**

/source/logs/nginx/20160101/**

/source/logs/web/20160101/**

![1567255053077](../%E6%AD%A6%E5%8A%9F%E7%A7%98%E7%B1%8D/flume.assets/1567255053077.png)

分析：

在A,B服务器上的/root/data有数据文件access.log、nginx.log、web.logr日志文件，AB采集后sink到C服务器，C再将数据sink到自己的hdfs中

在A,B服务器上配置

```properties
# Name the components on this agent
a1.sources = r1 r2 r3
a1.sinks = k1
a1.channels = c1

# Describe/configure the source
a1.sources.r1.type = exec
a1.sources.r1.command = tail -F /root/data/access.log
a1.sources.r1.interceptors = i1
a1.sources.r1.interceptors.i1.type = static
##  static拦截器的功能就是往采集到的数据的header中插入自##	 己定义的key-value对
a1.sources.r1.interceptors.i1.key = type
a1.sources.r1.interceptors.i1.value = access

a1.sources.r2.type = exec
a1.sources.r2.command = tail -F /root/data/nginx.log
a1.sources.r2.interceptors = i2
a1.sources.r2.interceptors.i2.type = static
a1.sources.r2.interceptors.i2.key = type
a1.sources.r2.interceptors.i2.value = nginx
a1.sources.r3.type = exec
a1.sources.r3.command = tail -F /root/data/web.log
a1.sources.r3.interceptors = i3
a1.sources.r3.interceptors.i3.type = static
a1.sources.r3.interceptors.i3.key = type
a1.sources.r3.interceptors.i3.value = web

# Describe the sink
a1.sinks.k1.type = avro
a1.sinks.k1.hostname = hdp3
a1.sinks.k1.port = 41414

# Use a channel which buffers events in memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 20000
a1.channels.c1.transactionCapacity = 10000

# Bind the source and sink to the channel
a1.sources.r1.channels = c1
a1.sources.r2.channels = c1
a1.sources.r3.channels = c1
a1.sinks.k1.channel = c1
```

服务器C上配置

```properties
#定义agent名， source、channel、sink的名称
a1.sources = r1
a1.sinks = k1
a1.channels = c1

#定义source
a1.sources.r1.type = avro
a1.sources.r1.bind = hdp3
a1.sources.r1.port =41414

#添加时间拦截器
a1.sources.r1.interceptors = i1
a1.sources.r1.interceptors.i1.type=org.apache.flume.interceptor.TimestampInterceptor$Builder

#定义channels
a1.channels.c1.type = memory
a1.channels.c1.capacity = 20000
a1.channels.c1.transactionCapacity = 10000

#定义sink
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path=hdfs://hdp3:8020/source/logs/%{type}/%Y%m%d
a1.sinks.k1.hdfs.filePrefix =events
a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
#时间类型
a1.sinks.k1.hdfs.useLocalTimeStamp = true
#生成的文件不按条数生成
a1.sinks.k1.hdfs.rollCount = 0
#生成的文件按时间生成
a1.sinks.k1.hdfs.rollInterval = 30
#生成的文件按大小生成
a1.sinks.k1.hdfs.rollSize  = 10485760
#批量写入hdfs的个数
a1.sinks.k1.hdfs.batchSize = 10000
flume操作hdfs的线程数（包括新建，写入等）
a1.sinks.k1.hdfs.threadsPoolSize=10
#操作hdfs超时时间
a1.sinks.k1.hdfs.callTimeout=30000

#组装source、channel、sink
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

先启动服务器C上的flume，如果先启动ab找不到sink的地址会报错

```shell
bin/flume-ng agent -c conf -f conf/avro_source_hdfs_sink.conf -name a1 -Dflume.root.logger=DEBUG,console
```

启动服务器A,B的flume

```shell
bin/flume-ng agent -c conf -f conf/exec_source_avro_sink.conf -name a1 -Dflume.root.logger=DEBUG,console	
```







# Azkaban

## 1.概述

azkaban是一款工作流任务调度工具，用于在一个工作流内以一个特定的顺序运行一组工作和流程。Azkaban使用job配置文件建立任务之间的依赖关系。

**工作流：**指“业务过程的部分或整体在计算机应用环境下的自动化”。工作流解决的主要问题是：为了实现某个业务目标，利用计算机软件在多个参与者之间按某种预定规则自动传递文档、信息或者任务。

## 2.Azkaban原理架构

![1567427053116](hadoop生态圈组件.assets/1567427053116.png)

mysql服务器: 存储元数据，如项目名称、项目描述、项目权限、任务状态、SLA规则等

AzkabanWebServer:对外提供web服务，使用户可以通过web页面管理。职责包括项目管理、权限授权、任务调度、监控executor。

AzkabanExecutorServer:负责具体的工作流的提交、执行。

## 3.two-server模式部署

### 3.1节点规划

| Host |          任务          |
| :--: | :--------------------: |
| hdp2 | web‐server,exec‐server |
| hdp3 |         mysql          |

### 3.2mysql配置初始化

hdp3：

~~~
mkdir /export/servers/azkaban
tar -zxvf azkaban-db-0.1.0-SNAPSHOT.tar.gz –C /export/servers/azkaban/
~~~

启动mysql：

~~~
mysql> CREATE DATABASE azkaban_two_server; #创建数据库
mysql> use azkaban_two_server;
mysql> source /export/servers/azkaban/azkaban-db-0.1.0-SNAPSHOT/create-all-sql-0.1.0-SNAPSHOT.sql;
~~~

### 3.3 web-server服务器配置

hdp2:

~~~
mkdir /export/servers/azkaban
tar -zxvf azkaban-web-server-0.1.0-SNAPSHOT.tar.gz –C /export/servers/azkaban/
tar -zxvf azkaban-exec-server-0.1.0-SNAPSHOT.tar.gz –C /export/servers/azkaban/
~~~

生成ssl证书：

~~~
keytool -keystore keystore -alias jetty -genkey -keyalg RSA
~~~

运行命令后提示输入当前生成的keystore密码，完成后将生成的keystore证书文件拷贝到azkaban web服务器根目录中。

配置azkaban.properties

~~~properties
# Azkaban Personalization Settings
azkaban.name=Test
azkaban.label=My Local Azkaban
azkaban.color=#FF3601
azkaban.default.servlet.path=/index
web.resource.dir=web/
default.timezone.id=Asia/Shanghai # 时区注意后面不要有空格

# Azkaban UserManager class
user.manager.class=azkaban.user.XmlUserManager
user.manager.xml.file=conf/azkaban-users.xml

# Azkaban Jetty server properties. 开启使用ssl 并且知道端口
jetty.use.ssl=true
jetty.ssl.port=8443
jetty.maxThreads=25

# Azkaban Executor settings  指定本机Executor的运行端口
executor.host=localhost
executor.port=12321

#  KeyStore for SSL ssl相关配置  注意密码和证书路径
jetty.keystore=keystore
jetty.password=123456
jetty.keypassword=123456
jetty.truststore=keystore
jetty.trustpassword=123456

# Azkaban mysql settings by default. Users should configure their own username and password.
database.type=mysql
mysql.port=3306
mysql.host=node-1
mysql.database=azkaban_two_server
mysql.user=root
mysql.password=hadoop
mysql.numconnections=100

#Multiple Executor 设置为false
azkaban.use.multiple.executors=true
#azkaban.executorselector.filters=StaticRemainingFlowSize,MinimumFreeMemory,CpuStatus 
azkaban.executorselector.comparator.NumberOfAssignedFlowComparator=1
azkaban.executorselector.comparator.Memory=1
azkaban.executorselector.comparator.LastDispatched=1
azkaban.executorselector.comparator.CpuUsage=1
~~~

添加一个配置文件

 在webserver的根目录下执行

```
[root@node-2 webserver]# mkdir -p plugins/jobtypes
[root@node-2 webserver]# cd plugins/jobtypes/
[root@node-2 jobtypes]# vim commonprivate.properties
azkaban.native.lib=false
execute.as.user=false
memCheck.enabled=false
```

### 3.4 exec-server服务器配置

配置conf/azkaban.properties：

~~~properties
# Azkaban Personalization Settings
azkaban.name=Test
azkaban.label=My Local Azkaban
azkaban.color=#FF3601
azkaban.default.servlet.path=/index
web.resource.dir=web/
default.timezone.id=Asia/Shanghai

# Azkaban UserManager class
user.manager.class=azkaban.user.XmlUserManager
user.manager.xml.file=conf/azkaban-users.xml
# Loader for projects
executor.global.properties=conf/global.properties
azkaban.project.dir=projects
# Velocity dev mode
velocity.dev.mode=false
# Azkaban Jetty server properties.
jetty.use.ssl=false
jetty.maxThreads=25
jetty.port=8081

# Where the Azkaban web server is located
azkaban.webserver.url=https://node-2:8443

# mail settings
mail.sender=
mail.host=
# User facing web server configurations used to construct the user facing server URLs. They are useful when there is a reverse proxy between Azkaban web servers and users.
# enduser -> myazkabanhost:443 -> proxy -> localhost:8081
# when this parameters set then these parameters are used to generate email links.
# if these parameters are not set then jetty.hostname, and jetty.port(if ssl configured jetty.ssl.port) are used.
# azkaban.webserver.external_hostname=myazkabanhost.com
# azkaban.webserver.external_ssl_port=443
# azkaban.webserver.external_port=8081
job.failure.email=
job.success.email=
lockdown.create.projects=false
cache.directory=cache
# JMX stats
jetty.connector.stats=true
executor.connector.stats=true
# Azkaban plugin settings
azkaban.jobtype.plugin.dir=plugins/jobtypes

# Azkaban mysql settings by default. Users should configure their own username and password.
database.type=mysql
mysql.port=3306
mysql.host=hdp2
mysql.database=azkaban_two_server
mysql.user=root
mysql.password=root
mysql.numconnections=100

# Azkaban Executor settings
executor.maxThreads=50
executor.port=12321
executor.flow.threads=30
~~~

### 3.5 集群启动

启动顺序（先启动exec 再启动webserver）**需要在对应模块的根目录下启动**

exec服务器启动之后 需要手动的激活  **每次启动都需要激活**

```
 bin/start-exec.sh 
 
 激活executor服务器
 curl -G "hdp2:$(<./executor.port)/executor?action=activate" && echo
 或者手动去数据库中把executors表中active字段改为1  0表示未激活 1 表示激活
 
 bin/start-web.sh
```

## 4.multiple-executor模式部署

multiple-executor模式是多个executor Server分布在不同服务器上，只需要将azkaban-exec-server安装包拷贝到不同机器上即可组成分布式。

scp executor server安装包到hdp1

~~~
cd  /export/servers/azkaban/execserver
curl -G "hdp1:$(<./executor.port)/executor?action=activate" && echo
~~~



