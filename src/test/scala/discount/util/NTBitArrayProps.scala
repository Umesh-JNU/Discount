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

package discount.util

import discount.TestGenerators._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import java.nio.ByteBuffer

class NTBitArrayProps extends AnyFunSuite with ScalaCheckPropertyChecks {
  import BitRepresentation._

  test("length") {
    forAll(dnaStrings) { x =>
      NTBitArray.encode(x).size should equal(x.length)
    }
  }

  test("decoding") {
    forAll(dnaStrings) { x =>
      NTBitArray.encode(x).toString should equal(x)
    }
  }

  test("copyPart identity") {
    forAll(dnaStrings) { x =>
      val enc = NTBitArray.encode(x)
      val buf = enc.partAsLongArray(0, enc.size)
      java.util.Arrays.equals(buf, enc.data) should be(true)
    }
  }

  test("k-mers length") {
    forAll(dnaStrings, ks) { (x, k) =>
      whenever (k <= x.length) {
        val kmers = KmerTable.fromSegment(NTBitArray.encode(x), k, false)
        kmers.size should equal((x.length - (k - 1)))
      }
    }
  }

  test("k-mers data") {
    forAll(dnaStrings, ks) { (x, k) =>
      whenever (k <= x.length && k >= 1 && x.length >= 1) {
        val kmers = NTBitArray.encode(x).kmersAsLongArrays(k, false).toArray
        val bb = ByteBuffer.allocate(32)
        val sb = new StringBuilder
        val kmerStrings = kmers.map(km => NTBitArray.longsToString(bb, sb, km, 0, k))
        kmerStrings.toList should equal (x.sliding(k).toList)
      }
    }
  }

  test("shift k-mer left") {
    forAll(dnaStrings, ks, dnaLetterTwobits) { (x, k, letter) =>
      whenever (k <= x.length && k >= 1 && x.length >= 1) {
        val first = NTBitArray.encode(x).partAsLongArray(0, k)
        NTBitArray.shiftLongArrayKmerLeft(first, letter, k)
        val enc2 = NTBitArray.encode(x.substring(1, k) + twobitToChar(letter))
        java.util.Arrays.equals(first, enc2.data) should be(true)
      }
    }
  }
}
