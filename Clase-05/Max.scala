import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import scala.math._


val conf = new SparkConf().setAppName("TestApp").setMaster("local")
val sc = new SparkContext(conf)

val vertices: RDD[(VertexId, Int)] =
  sc.parallelize(Array(
    (1L, 3),
    (2L, 6),
    (3L, 2),
    (4L, 1),
    (5L, 10)
  )
  )

val relationships: RDD[Edge[Boolean]] =
  sc.parallelize(Array(
    Edge(1L, 2L, true),
    Edge(2L, 1L, true),
    Edge(2L, 4L, true),
    Edge(4L, 2L, true),
    Edge(3L, 2L, true),
    Edge(2L, 3L, true),
    Edge(3L, 4L, true),
    Edge(4L, 3L, true),
    Edge(1L, 5L, true),
    Edge(5L, 1L, true)
  )
  )

val graph = Graph(vertices, relationships)
val initial_graph = graph.mapVertices((id, attr) => (attr, -1))

initial_graph.vertices.collect.foreach(println)

val initialMsg = 9999

def vprog(vertexId: VertexId, value: (Int, Int), message: Int): (Int, Int) = {
  if (message == initialMsg) {
    value
  }
  else {
    (max(message, value._1), value._1)
  }
}

def sendMsg(triplet: EdgeTriplet[(Int, Int), Boolean]): Iterator[(VertexId, Int)] = {
  val sourceVertex = triplet.srcAttr
  if (sourceVertex._1 == sourceVertex._2) {
    Iterator.empty
  }
  else {
    Iterator((triplet.dstId, sourceVertex._1))
  }
}

def mergeMsg(msg1: Int, msg2: Int): Int = max(msg1, msg2)

val maxGraph = initial_graph.pregel(initialMsg,
  Int.MaxValue,
  EdgeDirection.Out)(
  vprog,
  sendMsg,
  mergeMsg)

maxGraph.vertices.collect.foreach(v => println(s"${v}"))


