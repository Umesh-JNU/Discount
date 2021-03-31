package discount.hash

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should._

class ShiftScannerTest extends AnyFunSuite with Matchers {
  test("basic") {
    val space = MotifSpace.ofLength(5, false)
    val test = "TGTCAGTGGTGCGGTCCCAGAAAGAGCCGTCTTTCATCTGGTAACGGTGGGTTACGTACTTGCCGTCGGCATCCTGTACAAAGCAACAGTTGGCGTCCTCG"
    val scanner = new ShiftScanner(space)
    scanner.allMatches(test).map(_.pattern).toList should equal(test.sliding(5).toList)
  }
}
