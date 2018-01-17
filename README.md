Distributed Algorithms
======================

Build

To see the logging message shown here, change logger's "edu.membership" level to "INFO" (in src/main/resources/log4j2.xml)

```
$ ./gradlew clean build
```

Run membership protocol for 3 nodes locally (in terminals T{1,2,3} respectively):

T1:
```
$ java -cp build/libs/distributed-algorithms.jar:lib/log4j-core-2.9.1.jar:lib/log4j-api-2.9.1.jar edu.membership.RemoteNode 10000
13:52:47.116 INFO  - [127.0.0.1:10000 T=1516193567112] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10000},ts=1516193567112,hb=0}
# now run T2
13:52:49.823 INFO  - [127.0.0.1:10000 T=1516193569822] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10001},ts=1516193569795,hb=0}
13:52:51.823 INFO  - [127.0.0.1:10000 T=1516193571823] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10002},ts=1516193571770,hb=0}
# Ctrl-C and go to T2
```

T2:
```
$ java -cp build/libs/distributed-algorithms.jar:lib/log4j-core-2.9.1.jar:lib/log4j-api-2.9.1.j edu.membership.RemoteNode 10001 127.0.0.1:10000
13:52:49.798 INFO  - [127.0.0.1:10001 T=1516193569795] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10001},ts=1516193569795,hb=0}
13:52:50.008 INFO  - [127.0.0.1:10001 T=1516193570007] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10000},ts=1516193569927,hb=28}
# now run terminal 3
13:52:52.008 INFO  - [127.0.0.1:10001 T=1516193572007] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10002},ts=1516193571823,hb=0}
13:52:58.306 INFO  - [127.0.0.1:10001 T=1516193578306] Member 127.0.0.1:10000 removed from member list
# Ctrl-C and go to T3
```

T3:
```
$ java -cp build/libs/distributed-algorithms.jar:lib/log4j-core-2.9.1.jar:lib/log4j-api-2.9.1.j edu.membership.RemoteNode 10002 127.0.0.1:10000
13:52:51.775 INFO  - [127.0.0.1:10002 T=1516193571770] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10002},ts=1516193571770,hb=0}
13:52:51.985 INFO  - [127.0.0.1:10002 T=1516193571985] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10000},ts=1516193571922,hb=48}
13:52:51.986 INFO  - [127.0.0.1:10002 T=1516193571985] Added new member Member{addr=RemoteAddress{addr=[127, 0, 0, 1], port=10001},ts=1516193571823,hb=20}
# go to terminal 1
13:52:58.385 INFO  - [127.0.0.1:10002 T=1516193578384] Member 127.0.0.1:10000 removed from member list
13:53:04.985 INFO  - [127.0.0.1:10002 T=1516193584985] Member 127.0.0.1:10001 removed from member list
# Ctrl-C
```

What was that?

All nodes detected events of:
- new member being joined ("Added new member ...")
- and failed ("... removed from member list")
