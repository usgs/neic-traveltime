[![Codacy Badge](https://api.codacy.com/project/badge/Grade/04492b4350e744c6bf2ecc3560a66a5a)](https://app.codacy.com/app/jpatton-USGS/neic-traveltime?utm_source=github.com&utm_medium=referral&utm_content=usgs/neic-traveltime&utm_campaign=badger)
[![Build Status](https://travis-ci.org/usgs/neic-traveltime.svg?branch=master)](https://travis-ci.org/usgs/neic-traveltime)
[![Documentation](https://usgs.github.io/neic-traveltime/codedocumented.svg)](https://usgs.github.io/neic-traveltime/)

# neic-traveltime
Port of the NEIC 'TTimes' libraries from FORTRAN77 to Java.

Dependencies
------
* neic-traveltime was written in Oracle Java 1.8
* neic-traveltime is built with [Apache Ant](http://ant.apache.org/), and was
written using Eclipse and netbeans.  Netbeans project files, source files,
and an ant build.xml are included in this project

Building
------
The steps to get and build neic-traveltime.jar using ant are as follows:

1. Clone neic-traveltime.
2. Open a command window and change directories to /neic-traveltime/
3. To build the jar file, run the command `ant jar`
4. To generate javadocs, run the command `ant javadoc`
5. To compile, generate javadocs, build jar, run the command `ant all`

Using
-----
Once you are able to build the neic-traveltime jar, simply include the jar
file in your application.

Further Information and Documentation
------
For further information and documentation please check out the [neic-traveltime Documentation Site](https://usgs.github.io/neic-traveltime/).

File bug reports, feature requests and questions using [GitHub Issues](https://github.com/usgs/neic-traveltime/issues)
