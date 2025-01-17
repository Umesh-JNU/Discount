/*
 * This file is part of Discount. Copyright (c) 2021 Johan Nyström-Persson.
 *
 * Discount is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discount is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Discount.  If not, see <https://www.gnu.org/licenses/>.
 */

package discount.spark

import discount.bucket.BucketStats
import discount.{Abundance, NTSeq}
import discount.hash.{BucketId, MotifExtractor}
import discount.spark.Counting.countsFromSequences
import discount.util.{KmerTable, NTBitArray, ZeroNTBitArray}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.functions.collect_list
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.collection.mutable

final case class HashSegment(hash: BucketId, segment: ZeroNTBitArray)

object GroupedSegments {

  def hashSegments(input: Dataset[NTSeq], spl: Broadcast[MotifExtractor])
                     (implicit spark: SparkSession): Dataset[HashSegment] = {
    import spark.sqlContext.implicits._
    val splitter = spl.value
    for {
      r <- input
      (h, s) <- splitter.split(r)
      r = HashSegment(splitter.compact(h), NTBitArray.encode(s))
    } yield r
  }

  def fromReads(input: Dataset[NTSeq], spl: Broadcast[MotifExtractor])(implicit spark: SparkSession):
    GroupedSegments =
    new GroupedSegments(segmentsByHash(hashSegments(input, spl)), spl)


  def segmentsByHash(segments: Dataset[HashSegment])(implicit spark: SparkSession):
  Dataset[(BucketId, Array[ZeroNTBitArray])] = {
    import spark.sqlContext.implicits._
    val grouped = segments.groupBy($"hash")
    grouped.agg(collect_list($"segment")).as[(BucketId, Array[ZeroNTBitArray])]
  }
}

/**
 * A set of super-mers grouped into bins.
 * @param segments The super-mers in binary format
 * @param splitter The read splitter
 * @param spark
 * @tparam H Type of the k-mer hash
 */
class GroupedSegments(val segments: Dataset[(BucketId, Array[ZeroNTBitArray])],
                         val splitter: Broadcast[MotifExtractor])(implicit spark: SparkSession)  {
  import org.apache.spark.sql._
  import spark.sqlContext.implicits._

  def cache(): this.type = { segments.cache(); this }
  def unpersist(): this.type = { segments.unpersist(); this }

  def superkmerStrings: Dataset[(String, NTSeq)] = {
    val bcSplit = splitter
    segments.map(seg => {
      (bcSplit.value.humanReadable(seg._1),
        seg._2.map(_.toString()).mkString("\n  "))
    })
  }

  def writeSuperkmerStrings(outputLocation: String) = {
    superkmerStrings.write.mode(SaveMode.Overwrite).option("sep", "\t").
      csv(s"${outputLocation}_superkmers")
  }

  /**
   * In the haystack (this object), find only the buckets that potentially contain k-mers in the "needle" sequences
   * by joining with the latter's hashes.
   * The orientation of the needles will not be normalized even if the index is.
   * @return triples of (hash, haystack segments, needle segments)
   */
  def joinMatchingBuckets(needles: Iterable[NTSeq])(implicit spark: SparkSession):
    Dataset[(BucketId, Array[ZeroNTBitArray], Array[ZeroNTBitArray])] = {
    import spark.implicits._
    val needleSegments = GroupedSegments.fromReads(spark.sparkContext.parallelize(needles.toSeq).toDS(), splitter)
    val needlesByHash = needleSegments.segments
    segments.join(needlesByHash, "hash").
      toDF("hash", "haystack", "needle").as[(BucketId, Array[ZeroNTBitArray], Array[ZeroNTBitArray])]
  }

  /**
   * In the hashed buckets "haystack" (this object), find only the k-mers also present in the "needles" sequences.
   * @return The encoded k-mers that were present both in the haystack and in the needles, and their
   *         abundances.
   */
  def findKmerCounts(needles: Iterable[NTSeq], unifyRC: Boolean = false)(implicit spark: SparkSession): CountedKmers = {
    import spark.implicits._
    val buckets = joinMatchingBuckets(needles)
    val k = splitter.value.k
    val counts = buckets.flatMap { case (id, haystack, needle) => {
      val hsCounted = Counting.countsFromSequences(haystack, k, unifyRC)

      //toSeq for equality (doesn't work for plain arrays)
      //note: this could be faster by sorting and traversing two iterators jointly
      val needleTable = KmerTable.fromSegments(needle, k, unifyRC)
      val needleKmers = needleTable.countedKmers.map(_._1)
      val needleSet = mutable.Set() ++ needleKmers.map(_.toSeq)
      hsCounted.filter(h => needleSet.contains(h._1))
    } }
    new CountedKmers(counts, splitter)
  }


  class Counting(minCount: Option[Abundance], maxCount: Option[Abundance], filterOrientation: Boolean) {
    val countFilter = new CountFilter(minCount, maxCount)

    def bucketStats: Dataset[BucketStats] = {
      val k = splitter.value.k
      val f = countFilter
      val bcSplit = splitter
      val normalize = filterOrientation
      segments.map { case (hash, segments) => {
        val counted = countsFromSequences(segments, k, normalize).filter(f.filter)
        val stats = BucketStats.collectFromCounts(bcSplit.value.humanReadable(hash),
          counted.map(_._2))
        stats.copy(superKmers = segments.length)
      } }
    }

    def writeBucketStats(location: String): Unit = {
      val bkts = bucketStats
      bkts.cache()
      bkts.write.mode(SaveMode.Overwrite).option("sep", "\t").csv(s"${location}_bucketStats")
      Counting.showStats(bkts)
      bkts.unpersist()
    }

    /**
     * Convert segments (superkmers) into counted k-mers
     */
    def counts: CountedKmers = {
      val k = splitter.value.k
      val f = countFilter
      val normalize = filterOrientation
      val counts = segments.flatMap { case (hash, segments) => {
        countsFromSequences(segments, k, normalize).filter(f.filter)
      } }
      new CountedKmers(counts, splitter)
    }
  }

  def counting(minCount: Option[Abundance] = None, maxCount: Option[Abundance] = None,
               filterOrientation: Boolean = false) =
    new Counting(minCount, maxCount, filterOrientation)
}
