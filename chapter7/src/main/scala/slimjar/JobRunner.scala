package slimjar

import com.twitter.scalding._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop
import org.slf4j.LoggerFactory

object JobRunner {
  val log = LoggerFactory.getLogger(this.getClass.getName)

  def main(args : Array[String]) {
    hadoop.util.ToolRunner.run(getJobConfig(Args(args)), new Tool, args)
  }

  implicit class EnhancedConfiguration(val conf: Configuration) extends AnyRef {

    // Method to increase the heap size in case the --heapInc parameter is used
    def increaseJvmHeap(args: Args) : Configuration = {
      if (args.boolean("heapInc")) {
        log.info("Setting JVM Memory/Heap Size for every child mapper and reducer")

        val jvmOpts = "-Xmx4096m -XX:+PrintGCDetails -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=50"
        log.info("**** JVM Options : " + jvmOpts )

        conf.set("mapred.child.java.opts", jvmOpts)
      }
      conf
    }

    // Method to load external libraries into the Distributed Cache
    def addDependenciesToSharedChache(args: Args) : Configuration = {
      Mode(args, conf) match {
        case hadoop: HadoopMode =>
          log.info("-Hadoop Mode-")
          args.optional("libjars") foreach { hadoopPath =>
            log.info("Distributed Cache location => " + hadoopPath)
            JobLibLoader.loadJars(hadoopPath, conf)
            conf.addResource(hadoopPath)
          }
        case _ => { log.info("In Local Mode") }
      }
      conf
    }

  }

  def getJobConfig(args : Args): Configuration =
    (new Configuration)
      .increaseJvmHeap(args)
      .addDependenciesToSharedChache(args)

}