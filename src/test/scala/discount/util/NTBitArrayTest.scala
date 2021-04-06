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

import org.scalacheck.{Gen, Prop, Properties}
import Prop._
import discount.TestGenerators

import java.nio.ByteBuffer

class NTBitArrayTest extends Properties("NTBitArray") {
  import TestGenerators._
  import BitRepresentation._

  property("length") = forAll(dnaStrings) { x =>
    NTBitArray.encode(x).size == x.length
  }

  property("decoding") = forAll(dnaStrings) { x =>
    NTBitArray.encode(x).toString == x
  }

  property("copyPart identity") = forAll(dnaStrings) { x =>
    val enc = NTBitArray.encode(x)
    val buf = enc.partAsLongArray(0, enc.size)
    java.util.Arrays.equals(buf, enc.data)
  }

  property("k-mers length") = forAll(dnaStrings, ks) { (x, k) =>
    (k <= x.length) ==> {
      val kmers = NTBitArray.encode(x).kmersAsLongArrays(k, false).toArray
      kmers.size == (x.length - (k - 1))
    }
  }

  property("k-mers data") = forAll(dnaStrings, ks) { (x, k) =>
    (k <= x.length && k >= 1 && x.length >= 1) ==> {
      val kmers = NTBitArray.encode(x).kmersAsLongArrays(k, false).toArray
      val bb = ByteBuffer.allocate(32)
      val sb = new StringBuilder
      val kmerStrings = kmers.map(km => NTBitArray.longsToString(bb, sb, km, 0, k))
      kmerStrings.toList == x.sliding(k).toList
    }
  }

  property("shift k-mer left") = forAll(dnaStrings, ks, dnaLetterTwobits) { (x, k, letter) =>
    (k <= x.length && k >= 1 && x.length >= 1) ==> {
      val first = NTBitArray.encode(x).kmersAsLongArrays(k, false).next
      val shifted = NTBitArray.shiftLongArrayKmerLeft(first, letter, k)
      val enc2 = NTBitArray.encode(x.substring(1, k) + twobitToChar(letter))
      java.util.Arrays.equals(shifted, enc2.data)
    }
  }
}
