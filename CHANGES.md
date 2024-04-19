# Changes

This fork contains a number of contributions to the utp4j library, originally developped by Ivan Iljkic. The contributions are as follows:
- expanded the functionality and fixing of small bugs of existing `configTestRead.java` and `configTestWrite.java`:
    - corrected transfer speed calculations
    - exposed more utp algorithm parameters to be changes is the testPlan
- expanded test suite for `LEDBAT` congestion control mechanism (see [PacketTimeoutTest](src/test/java/net/utp4j/channels/impl/alg/PacketTimeoutTest.java), [LatencyFactorTest](src/test/java/net/utp4j/channels/impl/alg/LatencyFactorTest.java))
- added a test script to easily benchmark utp4j performance locally with network emulation, to simulate realistic network behaviour (see benchmarking directory)
- added more test configurations for benchmarking different values of UTP parameters
- minor improvements in readibility of source code, mainly in [UtpAlgorithm.java](src/main/java/net/utp4j/channels/impl/alg/UtpAlgorithm.java)

## Benchmarking
The benchmarking directory now contains a WIP script for benchmarking UTP4j performance

### How to run
The benchmark can currently only be run on Linux. In order to run the benchmark, the following must be installed:
- Java 1.8+
- iproute 2
- maven 3

First, it is advised to do a clean install of the library, by running:

`mvn clean`
`mvn compile` 

To then run the benchmarks, run: 

`benchmark.sh -p <path/to/testplan.csv> -f <size of file to transfer> -r <path/for/results/file.csv> -d <benchmark latency> -j <benchmark jitter> -l <packet loss>`

Here the user needs to provide the following:
- -p: the location of the test plan to use. See Benchmark configurations for an overview of the provided testplans.
- -f: the size of the file to transfer during utp benchmarking. E.g. 5MB or 800KB.
- -r: the location where to store the benchmarking result logs
- -d: the one-way latency from sender and receiver to use during the benchmark. E.g. 0ms or 100ms.
- -j: the jitter to use during the benchmark. E.g. 1ms or 10ms.
- -l: the packetloss to use during the benchmark. E.g. 1% or 0.1%.

As an example, going inside the benchmarking directory and running:

`./benchmark.sh -p ../testPlan/testplan.csv -f 5MB -r results.csv -d 100ms -j 10ms -l 1%`

runs the benchmarks using the testplan found at `../testPlan/testplan.csv` and will use a filesize of 5 MB during the benchmark. The benchmark will be run with a one-way latency of 100ms, a jitter of 10ms and 1% packet loss. The results of the benchmark will be stored in results.csv.

### Benchmark configurations
This repository contains a number of pre-made UTP4J benchmark test configurations. The following test configuration CSV files can be found inside of the testPlan directory:
- *target_buffering_delay_benchmark.csv*: Test UTP configurations with target buffering delays (C_CNTRL_TARGET_MICROS) varying between 10ms to 300ms. As long as the estimated buffering delay stays below the target, the UTP4J algorithm will try to increase the congestion window. A higher target buffering delay makes the UTP algorithm more agressive, but might cause high congestion.
- *fast_resend_benchmark.csv*: Test UTP configurations with the number of skipped acknowledgements needed for fast retransmit (MAX_SKIP_RESEND) varying between 2 and 30. A low number of skipped acknowledgements might make UTP unnecessarily resend packages, while a high number of skipped acknowledgements might lead to long retransmission times.
- *pkt_size_benchmark.csv*: Test UTP configurations with varying packet sizes (MAX_PKT_SIZE, MIN_PKT_SIZE) and packet size modes (PKT_SIZE_MODE).
- *congestion_window_increase_benchmark.csv*: Test UTP configurations with the maximum congestion window varying from 3 kB to 600 kB.
- *skip_packets_until_ack_benchmark.csv*: Test UTP configurations with differing selective acknowledgement parameters.

# Current shortcomings and Room for future improvement
Establishing multiple simultaneous connections is not easily possible in current version of the library. It requires implementing a custom `DatagramSocket` to distribute received packets between different `UtpReceiveRunnable` instances, as each runnable requires their own socket and normal datagram sockets cannot be shared. Implementing a native way of maintianing multiple connections would greatly increase the utility this library provides.

It is also worth noting that the current default parameters for UTP4J perhaps not optimal. Benchmarks were performed on a local computer with network emulation, which suggested that the maximum speed at which the congestion window can increase is vital for good performance, and could be set 10x higher than the default setting. However, more suitable UTP algorithm parameters could perhaps be found by taking into consideration modern hardware under modern network conditions and doing benchmarking in a more realistic setting.

# SEE ALSO
| Name | Link |
| :-: | :-: |
|  Main project issue   |  https://github.com/Tribler/tribler/issues/7911  |
| Main app description | https://github.com/Eragoneq/trustchain-superapp/blob/master/doc/BlockchainNetworkingProject/UtpTesting.md |
| Benchmarking done over 10 weeks | https://github.com/Eragoneq/trustchain-superapp/blob/master/doc/BlockchainNetworkingProject/Benchmarking.md |
| utp4j changes and benchmarks | https://github.com/PieterCarton/utp4j/blob/master/CHANGES.md |
| kotlin-ipv8 changes | https://github.com/Eragoneq/kotlin-ipv8/blob/master/doc/UtpBinaryTransfer.md |
