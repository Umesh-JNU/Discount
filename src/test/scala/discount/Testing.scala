/*
 * This file is part of Discount. Copyright (c) 2020 Johan Nyström-Persson.
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

package discount

import scala.collection.Seq
import discount.hash.MotifSpace
import discount.util.BitRepresentation
import org.scalacheck.Gen

object Testing {

  val all2mersTestOrder = Seq("AT", "AG", "CT", "GG", "CC",
    "AC", "GT", "GA", "TC",
    "CG", "GC",
    "TG", "CA", "TA",
    "TT", "AA")

  val space = MotifSpace.using(all2mersTestOrder)
//
  def m(code: String, pos: Int) = space.get(code, pos)
  def ms(motifs: Seq[(String, Int)]) = motifs.map(x => m(x._1, x._2))
}

object TestGenerators {
  import BitRepresentation._
  val dnaStrings: Gen[NTSeq] = for {
    length <- Gen.choose(1, 100)
    chars <- Gen.listOfN(length, dnaLetters)
    x = new String(chars.toArray)
  } yield x

  val ks: Gen[Int] = Gen.choose(2, 90)
  val ms: Gen[Int] = Gen.choose(1, 10)

  val dnaLetterTwobits: Gen[Byte] = Gen.choose(0, 3).map(x => twobits(x))
  val dnaLetters: Gen[Char] = dnaLetterTwobits.map(x => twobitToChar(x))
}