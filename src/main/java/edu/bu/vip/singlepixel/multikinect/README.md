# multikinect + singlepixellocalization
This directory contains a plugin for collecting single pixel sensor data with 
[multikinect](https://github.com/bu-vip/multikinect).

## Running
From the root of the repo, run:
```bash
bazel run //src/main/java/edu/bu/vip/singlepixel/multikinect:main -- --data_dir <directory-path>
```
For more information about the command line options, see the [MainArgs](MainArgs.java) class.