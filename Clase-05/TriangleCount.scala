import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


val vertexArray = Array(
  (1L, 1),
  (2L, 2),
  (3L, 3),
  (4L, 4),
  (5L, 5),
  (6L, 6),
  (7L, 7)
)
val edgeArray = Array(
  Edge(1L, 2L, true),
  Edge(2L, 3L, true),
  Edge(3L, 1L, true),
  Edge(3L, 4L, true),
  Edge(4L, 5L, true),
  Edge(5L, 6L, true),
  Edge(6L, 4L, true),
  Edge(7L, 3L, true)
)

val conf = new SparkConf().setAppName("TestApp").setMaster("local")
val sc = new SparkContext(conf)

val vertexRDD: RDD[(Long, Int)] = sc.parallelize(vertexArray)
val edgeRDD: RDD[Edge[Boolean]] = sc.parallelize(edgeArray)

val graph: Graph[Int, Boolean] = Graph(vertexRDD, edgeRDD)

val initialGraph = graph.mapVertices((id, attr) =>
  (attr, List((-1, -1, -1)), 1)
)

for (triplet <- initialGraph.triplets.collect) {
  println(s"${triplet.srcAttr} -" +
    s" ${triplet.attr} -> " + "" +
    s"${triplet.dstAttr}")
}

def vprog(id: VertexId, attr: (Int, List[(Int, Int, Int)], Int),
          neighbors: List[(Int, Int, Int)]): (Int, List[(Int, Int, Int)], Int) = {
  if (neighbors.equals(List((-1, -1, -1)))) {
    (attr._1, attr._2, attr._3)
  } else {
    (attr._1, neighbors, attr._3 + 1)
  }
}

def sendMsg(triplet: EdgeTriplet[(Int, List[(Int, Int, Int)], Int), Boolean]):
Iterator[(VertexId, List[(Int, Int, Int)])] = {
  var neighbors = List[(Int, Int, Int)]()
  if (triplet.srcAttr._3 == 1) {
    for(el <- triplet.srcAttr._2) {
      neighbors = neighbors ++ List((triplet.srcAttr._1, el._2, el._3))
    }
    Iterator((triplet.dstId, neighbors))
  } else if (triplet.srcAttr._3 == 2) {
    for(el <- triplet.srcAttr._2) {
      neighbors = neighbors ++ List((el._1, triplet.srcAttr._1, el._3))
    }
    Iterator((triplet.dstId, neighbors))
  } else if (triplet.srcAttr._3 == 3) {
    for(el <- triplet.srcAttr._2) {
      neighbors = neighbors ++ List((el._1, el._2, triplet.srcAttr._1))
    }
    Iterator((triplet.dstId, neighbors))
  } else {
    Iterator.empty
  }
}

def mergeMsg(a: List[(Int, Int, Int)], b: List[(Int, Int, Int)]): List[(Int, Int, Int)] = a ++ b

val triangleGraph = initialGraph.pregel(List((-1, -1, -1)), Int.MaxValue, EdgeDirection.Out)(
  vprog,
  sendMsg,
  mergeMsg
)

for (triplet <- triangleGraph.triplets.collect) {
  println(s"${triplet.srcAttr} -" +
    s" ${triplet.attr} -> " + "" +
    s"${triplet.dstAttr}")
}

var numberOfTriangles = 0
for(v <- triangleGraph.vertices.collect()) {
  for (p <- v._2._2) {
    if (p._1 == v._1) {
      numberOfTriangles += 1
    }
  }
}

println(s"El número de triángulos es igual a ${numberOfTriangles / 3}")

