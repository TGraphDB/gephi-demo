TGraph Demo
----------------
> A demonstration of [TGraph database](http://dx.doi.org/10.1145/2983323.2983335) build on top of Gephi (as a plugin).
>
> TGraph is a database designed for temporal graph data.

This Gephi plugin shows the power of TGraph database. It is a tiny traffic-query-system on a specific data set. The data set contains Beijing road network topological (about 110000 roads) and traffic data from 2010-11-04 00:00 to 2010-11-08 23:55 (about 1400 files, each contains traffic status of some or all roads at a specific time point)

This plugin also provide a Database-Builder to help you build a fresh TGraph database from raw data files.

## System requirement
- Java Runtime Environment version 8.0 or higher
- Gephi 0.9.1

# Plugin Installation

Download latest plugin from [here](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/gephi-plugin-tgraph-demo-1.0.0.nbm) (md5:`059fa4ec2c263c52742f12d0bdf91a4a`)

In Gephi, click menu `Tools` -> `Plugins` -> `Downloaded` -> `Add Plugins...`,
then choose the `gephi-plugin-tgraph-demo-1.0.0.nbm` and press `OK`.

Then check the `install` checkbox and press `Install` button. This plugin is not signed, install it at your risk.

# TGraph Database Installation
This plugin is packed with TGraph kernel (the database management system program) in. To get the data to play with, you have two options: to download an exist database from web, or build the database yourself from raw data. The former choice is quick and easy but only contains temporal data of 5 hours, while the later one provide you more choice.

### Download an exist DB
Download a tiny, out-of-the-box TGraph database which contains Beijing road network and traffic data from 2010-11-04 05:10 to 10:06 (about 231MB)
from [here](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/TGraph-demo-DB.tar) (md5 checksum:`5341788728d6260f9e0aabe5fd55d017`)

### Build DB from raw data
> 1. Build database from raw data requires extra 1~3GB memory in your computer.
> 2. You must install this plugin to Gephi to use this feature.

#### download raw data
- Beijing network topology, [Topo.csv](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/Topo.csv.gz)(md5:`587284f28e49143884f6253d7e1ec793`)
- Traffic data(at lease one, but more if you like) at:
  - [2010-11-04](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/20101104.tar.gz)(md5:`fbf555adb8fbc018c73016f31baed086`)
  - [2010-11-05](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/20101105.tar.gz)(md5:`989e76ad6e2606324c17804e4b3a72e3`)
  - [2010-11-06](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/20101106.tar.gz)(md5:`91999663ba1be49920ad7b4877c2a0cb`)
  - [2010-11-07](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/20101107.tar.gz)(md5:`fd0846aa8b2fb767d389beea040c404e`)
  - [2010-11-08](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/20101108.tar.gz)(md5:`4158bf28c3429b0a4ee0c5d8e434580b`)
- **Extra all compressed data**.

#### build database
1. In Gephi, click `Plugins`->`TGraph Demo DB Builder` to open the database builder.
2. Choose `Topo.csv` as `Network File`
3. Choose an empty folder as `Database Folder`
4. Choose `20101104` as `Temporal Data Folder` (if you want import multiple days, just put them in one folder and choose it)
5. Choose a period of time from `Time Range to import`
6. Click `Build Database` and wait. Import all traffic data will take about 40 minute and 6GB disk space.

# Play with data

[Here](https://youtu.be/nbWa_5OL3GU) is a video tutorial. [Download this video](http://mashuai.buaa.edu.cn/TGraphdemo.avi)


# Notes
1. **Disconnect from database BEFORE you close Gephi**, or the database may be **damaged**.

2. All demo features (except `DB Builder`) are only available when the database is connected.

3. Gephi may produce an `Run out of memory` notice to you when you `import` the road network to ask you enlarge the memory which Gephi could use in your computer. This is because the default max size of memory which Gephi can use is 512MB. You must have at least 2GB free memory for Gephi to play with an existing database. The more memory you give, the faster the program runs.

4. You may find the road network has some error (road overlap, or wrong angle/length), this is because the raw data does not contains the exact latitude and longitude of the road start/end point, only contains road topology, road length/angle. So when the network is drawn, it has such error. But it is good enough as a *demonstration*.

5. In the `Path Search` panel, when using the algorithm `Reachable Area`, you should also choose an end node (although it's meaningless) or you may hit an error.

6. To build this plugin from source code, please wait for our release of `TGraph-kernel` and `tgraph-temporal-storage` maven package.

7. Do remember to open an empty project in Gephi **BEFORE** you connect to a TGraph DB. Or Gephi may produce a NullPointerException Error.

8. Do check the `TGraph Window Heatmap` option in Gephi's `Render Manager` in the `preview` panel. Or you may hit unexpected error.

# Feed back
please use issue.

# About TGraph Database Management System
See our [demo paper](http://dx.doi.org/10.1145/2983323.2983335) published in CIKM2016.

# Change log
## 2016-10
1. Better database folder chooser when connecting to a database.
2. Fix Name error in Gephi plugin manager.
3. Packed with new TGraph kernel which is compatible with neo4j 2.2.3.

## 2016-06
A runnable version.