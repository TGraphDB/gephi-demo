## TGraph Demo

> A demonstration of TGraph database build on top of Gephi(as a plugin).

This Gephi plugin shows the power of TGraph database. It is a tiny traffic-query-system on a specific data set. The data set contains Beijing road network topological(about 110000 roads) and traffic data from 2010-11-01 00:00 to 2010-11-08 23:55 (about 1400 files, each contains traffic status of some or all roads at a specific time point)

This plugin also provide a Database-Builder to help you build a fresh TGraph database from raw data files.

# System requirement
- Gephi 0.9.1
- Maven (if you want to build this plugin from source)

# Plugin Installation
## download plugin
## build plugin from source
Download this plugin, and execute `mvn package` in project root directory, then `PROJECT_ROOT/modules/target/TGraphDemo/target/gephi-plugin-tgraph-demo-1.0.0.nbm` is the archived plugin. You can then install it in Gephi 

# TGraph Database Installation
### download an exist DB
### build DB from raw data
> 1. Build database from source requires extra 3GB memory in your computer.
> 2. You must install this plugin to Gephi to use this feature.

1. In Gephi, click `Plugins`->`TGraph Demo DB Builder` to open the database builder.
2. Choose an

## play with it


# Notes

The tool identify nodes by their labels, so your graph must have unique label for each node.If two or more node has same label, only one will be restored.

When restore from file, only the nodes which match the labels in file would be colored, others will remain their original color.

# Known issues
- you can only connect to ONE TGraph database in a Gephi session. To connect to another TGraph database, you must restart Gephi.

# Feed back
please use issue.
