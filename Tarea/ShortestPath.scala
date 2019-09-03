import org.apache.spark.graphx._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

val vertexArray = Array(
(1L, ("Santiago")),
(2L, ("Concepción")),
(3L, ("Temuco")),
(4L, ("Pucón")),
(5L, ("Osorno"))
)

val edgeArray = Array(
Edge(1L, 2L, 500),
Edge(1L, 3L, 680),
Edge(2L, 3L, 300),
Edge(3L, 4L, 105),
Edge(3L, 5L, 250),
Edge(4L, 5L, 240)
)

val conf = new SparkConf().setAppName("TestApp").setMaster("local")
val sc = new SparkContext(conf)

val vertexRDD: RDD[(Long, String)] = sc.parallelize(vertexArray)
val edgeRDD: RDD[Edge[Int]] = sc.parallelize(edgeArray)

val graph: Graph[String, Int] = Graph(vertexRDD, edgeRDD)

val initialNode: VertexId = 1
val initialGraph = graph.mapVertices((id, attr) =>
    if (id == initialNode) {
        (attr, 0.0)
    } else {
        (attr, Double.PositiveInfinity)
    }
)

for (triplet <- initialGraph.triplets.collect) {
println(s"${triplet.srcAttr} -" +
    s" ${triplet.attr} -> " + "" +
    s"${triplet.dstAttr}")
}

def vprog(id: VertexId, attr: (String, Double), newDistance: Double): (String, Double) = {
(attr._1, math.min(attr._2, newDistance))
}

def sendMsg(triplet: EdgeTriplet[(String, Double), Int]): Iterator[(VertexId, Double)] = {
    if (triplet.srcAttr._2 + triplet.attr < triplet.dstAttr._2) {
        Iterator((triplet.dstId, triplet.srcAttr._2 + triplet.attr))
    } else {
        Iterator.empty
    }
}

def mergeMsg(a: Double, b: Double): Double = math.min(a, b)

val shortestPathGraph = initialGraph.pregel(Double.PositiveInfinity, Int.MaxValue, EdgeDirection.Out)(
vprog,
sendMsg,
mergeMsg
)

for (triplet <- shortestPathGraph.triplets.collect) {
    println(s"${triplet.srcAttr} -" +
        s" ${triplet.attr} -> " + "" +
        s"${triplet.dstAttr}")
}


