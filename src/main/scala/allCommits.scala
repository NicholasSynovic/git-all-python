import ammonite.ops._
import ammonite.ops.ImplicitWd._
import scala.collection.mutable.ListBuffer
import scala.io.StdIn.readLine
import scopt.OptionParser

object allCommits {
  def main(args: Array[String]): Unit = {
    //Gets information about repo so that all commits can be cloned
    val config = parseCommandLine(args).getOrElse(Config())

    val repoURL = config.url.getOrElse("")
    println(repoURL)
    val sourceFolder = config.src.getOrElse("")
    val destinationFolder = config.dst.getOrElse("")

    //Establishes path for source folder where clone occurs and destination folder which will recieve every commit
    val sourcePath = home / "Desktop" / sourceFolder
    val destinationPath = home / "Desktop" / destinationFolder

    //Checks whether these two folders already exist and if so exits the program and alerts the user
    if (exists ! sourcePath) {
      println("Source folder already exists cannot execute program")
      System.exit(0)
    }
    if (exists ! destinationPath) {
      println("Destination folder already exists cannot execute program")
      System.exit(0)
    }

    //Clones repo into source folder
    %.git("clone", repoURL)(home / "Desktop")

    //Gets the hashs for each commit and prepares them as an iterator
    val logForIterator = hashCodes(sourceFolder)
    val logIterator = logForIterator.toIterator

    //Creates folder where all commits will be placed as subfolders
    mkdir ! home / "Desktop" / destinationFolder

    //This loop creates a new folder for each commit and fills it with the files that were current as of that commit
    for (currentHash <- logIterator) {
      mkdir ! destinationPath / currentHash
      var currentPath = destinationPath / currentHash
      %.git('init)(currentPath)
      %%("git", "remote", "add", "upstream", sourcePath)(currentPath)
      %%("git", "fetch", "upstream")(currentPath)
      %%("git", "checkout", currentHash)(currentPath)
    }
  }

  //This method takes the name of the folder where the repo is cloned and returns the hash codes
  def hashCodes(args: String): Array[String] = {
    val source = home / "Desktop" / args
    val log = %%("git", "log")(source)
    val logString = log.toString
    val logArray = logString.split("\n")
    val justHashCodes = logArray filter { line => line.startsWith("commit") } map { line => line.split(" ")(1) }

    return justHashCodes
  }

  def parseCommandLine(args: Array[String]): Option[Config] = {
    val parser = new scopt.OptionParser[Config]("scopt") {
      head("allCommits", "1.0")
      opt[String]('s', "src") action { (x, c) =>
        c.copy(src = Some(x))
      } text ("s/src is a String property")
      opt[String]('d', "dst") action { (x, c) =>
        c.copy(dst = Some(x))
      } text ("d/dst is a String property")
      opt[String]('u', "url") action { (x, c) =>
        c.copy(url = Some(x))
      } text ("u/url is a String property")
    }
    parser.parse(args, Config())
  }

  case class Config(
                     src: Option[String] = None,
                     dst: Option[String] = None,
                     url: Option[String] = None
                   )

}

