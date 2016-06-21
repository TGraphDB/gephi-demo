TGraph Demo
----------------
> A demonstration of TGraph database build on top of Gephi(as a plugin).

This Gephi plugin shows the power of TGraph database. It is a tiny traffic-query-system on a specific data set. The data set contains Beijing road network topological(about 110000 roads) and traffic data from 2010-11-04 00:00 to 2010-11-08 23:55 (about 1400 files, each contains traffic status of some or all roads at a specific time point)

This plugin also provide a Database-Builder to help you build a fresh TGraph database from raw data files.

## System requirement
- Java Runtime Environment version 8.0 or higher
- Gephi 0.9.1
- Maven (if you want to build this plugin from source)

# Plugin Installation
## download plugin
Download lastest plugin from [here](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo%2Fgephi-plugin-tgraph-demo-1.0.0.nbm)(md5:`1a0c6ec556b188c830d7f0f5c2e4ee00`)
## build plugin from source
Download the source code, and execute `mvn package` in project root directory, then `PROJECT_ROOT/modules/target/TGraphDemo/target/gephi-plugin-tgraph-demo-1.0.0.nbm` is the archived plugin. You can then install it in Gephi.

**Pre-requirements** `TGraph-kernel` and `TGraph-temporal-storage`, see `Notes` section.
## intall plugin to Gephi
In Gephi, click menu `Tools` -> `Plugins` -> `Downloaded` -> `Add Plugins...`,
then choose the `gephi-plugin-tgraph-demo-1.0.0.nbm` and press `OK`.

Then check the `install` checkbox and press `Install` button. This plugin is not signed, install it at your risk.
# TGraph Database Installation
### Download an exist DB
download a tiny, out-of-the-box TGraph database which contains Beijing road network and traffic data from 2010-11-04 05:16 to 10:06 (about 216MB)
from [here](7bvaq3.com1.z0.glb.clouddn.com/TGraph-demo-DB-without-log.tar)(md5 checksum:`297f46d5ab63ed35795ba853ad33b33a`)

### Build DB from raw data
> 1. Build database from raw data requires extra 3GB memory in your computer.
> 2. You must install this plugin to Gephi to use this feature.

#### download raw data
- Beijing network topologic, [Topo.csv](http://7bvaq3.com1.z0.glb.clouddn.com/TGraphDemo/Topo.csv.gz)(md5:`587284f28e49143884f6253d7e1ec793`)
- Traffic data(at lease one, but more if you would like) at:
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
6. Click `Build Database` and wait( import all traffic data will take about 40 minute and 6GB disk space )

# Play with data

[Here](https://youtu.be/nbWa_5OL3GU) is a video tutorial. [Download this video](http://mashuai.buaa.edu.cn/TGraphdemo.avi)


# Notes
1. **Disconnect from database BEFORE you close Gephi**, or the database may be **damaged**.

2. All demo features(except `DB Builder`) are only avaiable when the database is connected.

3. Gephi may produce an `Run out of memory` notice to you when you `import` the road network to ask you enlarge the memory which Gephi could use in your computer. This is because the default max size of memory which Gephi can use is 512MB. You must have at least 2GB free memory for Gephi to play with an existing database. The more memory you give, the faster the program runs.

4. You may find the road network has some error(road overlap, or wrong angle/length), this is because the raw data does not contains the exact latitude and longitude of the road start/end point, only contains road topologic, road length/angle. So when the network is drawn, it has such error. But it is good enough as a *demonstration*.

5. In the `Path Search` pannel, only the algorithm `Time Dependent Dijkstra` is implemented.

6. To build this plugin from source code, you must have installed `TGraph-kernel` and `tgraph-temporal-storage` to your local maven repo, but we have not publish these two program yet. So you'd better download the plugin we already package.

# Known issues
- you can only connect **ONE** time in a Gephi session. To connect to another TGraph database or the same TGraph database twice, you must restart Gephi.

# Feed back
please use issue.
