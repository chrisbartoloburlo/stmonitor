package monitor.util

import com.github.tototoshi.csv._
import java.io.File

class logger(pathname: String) {
  private val f = new File(f"${pathname}")
  private val writer: CSVWriter = CSVWriter.open(f)

  def log(info: String): Unit = {
    writer.writeRow(info.split(" "))
  }

  def log(info: List[String]): Unit ={
    for (i <- info){
      log(i)
    }
  }

}