## Overview

Discount is a Spark-based tool for k-mer counting, and for analysis of minimizer orderings.

This software includes [Fastdoop](https://github.com/umbfer/fastdoop) by U.F. Petrillo et al [1].
We have also included some of the [DOCKS](http://acgt.cs.tau.ac.il/docks/) sets generated by Y Orenstein et al [2].
 
 For more information, see our paper: https://www.biorxiv.org/content/10.1101/2020.10.12.335364v2
 
## Compiling (optional)

To compile the software, the SBT build tool (https://www.scala-sbt.org/) is needed. 
We recommend compiling on JDK 8.

The command `sbt package` will compile the software and produce the necessary jar file in 
target/scala-2.11/discount_2.11-1.0.0.jar. (You do not need to install Scala manually as sbt will handle this for you.)

If you prefer not to compile Discount by yourself, you can download a pre-built release.

## Running

First, install and configure Spark (http://spark.apache.org).
Discount has been developed and tested with JDK 8, Scala 2.11 and Spark 2.4.
(Note that Spark 3.0 is not compatible with Scala 2.11. Currently, any 2.4.x version should be fine, e.g. 2.4.7)

Spark applications, such as Discount, can run locally on your laptop, on a cluster, or in the "cloud". 
In Google Cloud, we have tested on Dataproc image version 1.4 (Debian 9, Hadoop 2.9, Spark 2.4).

Copy spark-submit.sh.template to spark-submit.sh and edit the necessary variables in the file.
Alternatively, if you submit to a GCloud cluster, you can use submit-gcloud.sh.template. In that case,
change the example commands below to use that script instead, and insert your GCloud cluster name as an additional first parameter when invoking.

## Usage (k-mer counting)

Example (statistical overview of a dataset) where k = 55, m = 10 (minimizer width)
 
`
./spark-submit.sh --motif-set DOCKS/res_10_50_4_0.txt -k 55 -m 10 /path/to/data.fastq stats
`

The above command uses the "universal frequency sampled" ordering with the supplied DOCKS motif set res_10_50_4_0. 
It can be used for any k >= 50. For 20 <= k < 50, res_10_20_4_0 should be used instead.
This motif set is used to split the input into evenly sized parts and has no effect on the final result. 

We recommend m = 10 in most cases. For other values of m, please obtain a DOCKS set using the link above.

All example commands shown here accept multiple input files. The FASTQ and FASTA formats are supported, 
and must be uncompressed.

Example to generate a full counts table output with k-mer sequences (in many cases larger than the input data):

`
./spark-submit.sh --motif-set DOCKS/res_10_50_4_0.txt -k 55 -m 10 /path/to/data.fastq count -o /path/to/output/dir --sequence
`

A new directory called /path/to/output/dir_counts will be created with the output.

Usage of upper and lower bounds filtering, histogram generation, and other functions, 
may be seen in the online help:

`
./spark-submit.sh --help
`

## Usage (minimizer ordering evaluation)

Discount can also be used to evaluate the efficiency of various minimizer orderings
and motif sets.

`
./spark-submit.sh --motif-set DOCKS/res_10_50_4_0.txt -k 55 -m 10 /path/to/data.fastq count -o /path/to/output/dir --write-stats
`

The --write-stats flag enables this mode. A new directory called /path/to/output/dir_stats will be created with the output.
Each line in the output file will represent a single k-mer bin. The output files will contain five columns, which are:
Number of superkmers, total number of k-mers, distinct k-mers, unique k-mers, maximum abundance for a single k-mer.
See the file BucketStats.scala for details.

The above example uses the universal frequency ordering (see the paper linked above).
 The commands below can be used to enable other orderings.

Universal set ordering (lexicographic)

`
./spark-submit.sh --motif-set DOCKS/res_10_50_4_0.txt -o lexicographic -k 55 -m 10 /path/to/data.fastq count -o /path/to/output/dir --write-stats
`

Minimizer signature

`
./spark-submit.sh -o signature -k 55 -m 10 /path/to/data.fastq count -o /path/to/output/dir --write-stats
`

The frequency counted (all 10-mers) ordering is the default if no other flags are supplied:

`
./spark-submit.sh -k 55 -m 10 /path/to/data.fastq count -o /path/to/output/dir --write-stats
`

## Tips
* If the input data contains reads longer than 1000 bp, you must use the --maxlen flag to specify the longest
expected single read length.

* Visiting http://localhost:4040 (if you run a standalone Spark cluster) in a browser will show progress details while Discount is running.

* If you are setting up Spark for the first time, you may want to configure key settings such as logging verbosity,
spark executor memory, and the local directories for shuffle data (may get large).
You can edit the files in e.g. spark-2.4.x-bin-hadoopX.X/conf/ to do this.

* You can speed up the sampling stage somewhat by setting the --numCPUs flag.

* The number of files generated in the output tables will correspond to the number of partitions Spark uses, which you can configure in the run scripts.
However, we recommend configuring partitions for performance/memory usage and manually joining the files later if you wish.

## References

1. Petrillo, U. F., Roscigno, G., Cattaneo, G., & Giancarlo, R. (2017). FASTdoop: A versatile and efficient library for the input of FASTA and FASTQ files for MapReduce Hadoop bioinformatics applications. Bioinformatics, 33(10), 1575–1577.
2. Orenstein, Y., Pellow, D., Marçais, G., Shamir, R., & Kingsford, C . (2017). Designing small universal k-mer hitting sets for improved analysis of high-throughput sequencing. PLoS Computational Biology, 13(10), 1–15. https://doi.org/10.1371/journal.pcbi.1005777
