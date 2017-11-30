import com.datastax.driver.core.ProtocolVersion
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.immutable.ListMap
import org.apache.log4j.{Level, Logger}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import  org.json4s.JObject
import org.json4s._
import org.json4s.JsonDSL
import org.json4s.native.{JsonMethods, JsonParser}
import org.json4s.DefaultFormats
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
//import net.liftweb.json._
object SparkStreamingAnalytics  {
  def main(args: Array[String]): Unit = {

    //Logger.getLogger("org").setLevel(Level.OFF)
    //Logger.getLogger("akka").setLevel(Level.OFF)

    val conf = new SparkConf(true)
      .setMaster("local[*]")
      .setAppName("RSVPStream").set("spark.cassandra.connection.host", "127.0.0.1")


    val ssc = new StreamingContext(conf, Seconds(10))

    val zk_quorum = "localhost:2181" // Zookeeper
    val group_id = "1"
    val topics = Map("meetup_stream" -> 1)
    // create a DStream from Kafka

    def printList[T](args: List[T]) = args.foreach(println)

    val json_stream = KafkaUtils.createStream(ssc, zk_quorum, group_id, topics).map(_._2)
     // .map{case(_,v) => implicit val formats = DefaultFormats
    //    JsonParser.parse(v).extract[RSVPData] }


     // .foreachRDD(rsvp =>



    //  rsvp.saveToCassandra("meetup","rsvpstream",SomeColumns("venue_name" as "venue","visibility","response","guests","member_name" as "member","rsvp_id","rsvp_last_modified_time" as "mtime",
     //   "event_name" as "event", "group_name" as "group")))
      //,SomeColumns("rsvp_id","event_name",event_time, event_url, group_city, group_country,group_id, group_lat,
      //group_lon, group_name, group_state, final_topics_list, guests, member_id,member_name,response,venue_id,venue_lat,venue_lon, venue_name, visibility,)))

   //json_stream.print()
   def convertJSONFormat (a: String) : JObject= {

     implicit val formats = DefaultFormats
     val each_rsvp = JsonParser.parse(a)
     val p = each_rsvp.extract[RSVPData]
     val mtime = p.mtime
     val rsvp_id=p.rsvp_id
     val event_name=p.event.event_name
     val event_time=p.event.event_time
     val event_url=p.event.event_url
     val group_city=p.group.group_city
     val group_country=p.group.group_country
     val group_id=p.group.group_id
     val group_lat=p.group.group_lat
     val group_lon=p.group.group_lon
     val group_name=p.group.group_name
     val group_state=p.group.group_state
     val guests=p.guests
     val member_id=p.member.member_id
     val member_name=p.member.member_name
     val response=p.response
     val group_topics = p.group.group_topics
     val visibility = p.visibility

     val venue= p.venue.getOrElse(default = None)
     val stringVenue = venue.toString.replaceAll("VenueDetails","")
     val venueCorrected = stringVenue.replaceAll("[()]", "")

     val venueArr = venueCorrected.split(",").reverse.toList

     //specify default values for venue:

     var venue_name=""
     var venue_lon=0.0
     var venue_lat =0.0
     var venue_id=0

     if(venue!=None ) {

       venue_id=venueArr(0).toInt
       venue_lat=venueArr(1).toDouble
       venue_lon =venueArr(2).toDouble

       val length=venueArr.length

       for (i <- (3 to length-1).reverse){
         venue_name=venue_name + venueArr(i)
         if(i<(length-1)){ venue_name = venue_name + ","}

       }


     }
     else
     {
       venue_name="Not Found"
       venue_lon=0.0
       venue_lat =0.0
       venue_id=0

     }

     //this will be JArray so converting JArray to List


     val topics_list = group_topics.extract[List[Map[String,String]]]

     var topics_list_mapped  = topics_list.flatten

     topics_list_mapped = topics_list_mapped.filter{case (a, b) => a.equals("topic_name")}
     var final_topics_list = topics_list_mapped.flatMap{ case (a,b) => List(a,b) }
     val removedString = "topic_name"
     final_topics_list=final_topics_list.filterNot(word => word == removedString)

     val rsvpMap =
       ("rsvp_id" -> rsvp_id )~
         ("event_name" -> event_name)~
         ("event_time" -> event_name)~
         ("event_url" -> event_url)~
         ("group_city" -> group_city)~
         ("group_country" -> group_country)~
         ("group_id" -> group_id)~
         ("group_lat" -> group_lat)~
         ("group_lon" -> group_lon)~
         ("group_name" ->group_name)~
         ("group_state"->group_state)~
         ("group_topic_names" -> final_topics_list)~
         ("guests" -> guests)~
         ("member_id" -> member_id)~
         ("member_name" -> member_name)~
         ("response" -> response)~
         ("mtime" -> mtime)~
         ("venue_id" -> venue_id)~
         ("venue_lat" -> venue_lat)~
         ("venue_lon" -> venue_lon)~
         ("venue_name" -> venue_name)~
         ("visibility" -> visibility)
      val JSONString = rsvpMap
      return JSONString
   }


    val convertedRSVPStream = json_stream.map(rsvp => convertJSONFormat(rsvp))
    convertedRSVPStream.print(5)

    convertedRSVPStream.map{rsvp => implicit val formats = DefaultFormats
    rsvp.extract[RSVPDetailsTest]
    }.foreachRDD(rsvp => rsvp.saveToCassandra("meetup","rsvpstream",SomeColumns("rsvp_id","event_name","event_time", "event_url",
      "group_city", "group_country","group_id", "group_lat","group_lon","group_name","group_state","group_topic_names"  ,"guests",
      "member_id","member_name", "response","rsvp_last_modified_time" as "mtime","venue_id", "venue_lat", "venue_lon", "venue_name", "visibility")))






    //Cassandra table
    //    CREATE TABLE meetup.rsvpstream (
    //    rsvp_id int PRIMARY KEY,
    //    event_name text,
    //    event_time timestamp,
    //    event_url text,
    //    group_city text,
    //    group_country text,
    //    group_id int,
    //    group_lat int,
    //    group_lon int,
    //    group_name text,
    //    group_state text,
    //    group_topic_names text,
    //    guests int,
    //    member_id int,
    //    member_name text,
    //    response text,
    //    rsvp_last_modified_time timestamp,
    //    venue_id int,
    //    venue_lat decimal,
    //    venue_lon decimal,
    //    venue_name text,
    //    visibility text
    //)

    ssc.start()
    ssc.awaitTermination()

  }

}
