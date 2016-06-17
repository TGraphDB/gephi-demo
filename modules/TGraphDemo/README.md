## TGraph Demo

> A demonstration of TGraph database build on top of Gephi(as a plugin).

This Gephi plugin shows the power of TGraph database. It is a tiny traffic-query-system on a specific data set. The data set contains Beijing road network topological(about 110000 roads) and traffic data from 2010-11-01 00:00 to 2010-11-08 23:55 (about 1400 files, each contains traffic status of some or all roads at a specific time point)

This plugin also provide a Database-Builder to help you build a fresh TGraph database from raw data files.

# System requirement
- Java Runtime Environment version 8.0 or higher
- Gephi 0.9.1
- Maven (if you want to build this plugin from source)

# Plugin Installation
## download plugin
## build plugin from source
Download this plugin, and execute `mvn package` in project root directory, then `PROJECT_ROOT/modules/target/TGraphDemo/target/gephi-plugin-tgraph-demo-1.0.0.nbm` is the archived plugin. You can then install it in Gephi 
## intall plugin to Gephi
In Gephi, click menu `Tools` -> `Plugins` -> `Downloaded` -> `Add Plugins...`,
then choose the `gephi-plugin-tgraph-demo-1.0.0.nbm` and press `OK`.

Then check the `install` checkbox and press `Install` button. This plugin is not signed, install it at your risk.
# TGraph Database Installation
### download an exist DB
download a tiny, off-the-shelf TGraph database which contains Beijing road network and traffic data from 2010-11-04 05:16 to 10:06 (about 216MB)
from [here](http://7bvaq3.com1.z0.glb.clouddn.com/amtf-without-log.tar)
### build DB from raw data
> 1. Build database from raw data requires extra 3GB memory in your computer.
> 2. You must install this plugin to Gephi to use this feature.

1. In Gephi, click `Plugins`->`TGraph Demo DB Builder` to open the database builder.
2. Choose an

# Play with me

[here](http://mashuai.buaa.edu.cn/TGraphdemo.avi) is a video tutorial


# Notes
Gephi may produce an `Run out of memory` notice to you when you `import` the road network to ask you enlarge the memory which Gephi could use in your computer. This is because the default max size of memory which Gephi can use is 512MB. Just enlarge it as big as possible.

# Known issues
- you can only connect to ONE TGraph database in a Gephi session. To connect to another TGraph database, you must restart Gephi.

# Feed back
please use issue.
