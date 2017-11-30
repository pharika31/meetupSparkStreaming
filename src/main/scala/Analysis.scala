import org.apache.spark.{SparkContext, SparkConf} // Spark
import com.datastax.spark.connector._ // Cassandra connector
import scala.collection.immutable.ListMap

import org.apache.log4j.Logger
import org.apache.log4j.Level

object Analysis {
  def main(args: Array[String]): Unit = {

    Logger.getLogger("org").setLevel(Level.OFF)

    // Configuration
    val conf = new SparkConf(true)
      .setAppName("Rsvp Analysis")
      .setMaster("local[*]")
      .set("spark.cassandra.connection.host", "127.0.0.1")

    val sc = new SparkContext(conf) // Create SparkContext
    

    // Read the data from cassandra
    val rsvp = sc.cassandraTable("meetup", "rsvpstream")
    rsvp.cache() // cache the RDD

    println("Total no of rows available in the data: " + rsvp.count())
    println(rsvp.first())

    val group_city_data = rsvp.select("group_city") // Get group city column data
    group_city_data.cache() // cache group city data

    val group_topics_data = rsvp.select("group_topic_names")
    group_city_data.cache()


    //// Overall no.of of unique group cities
    // Get distinct group_city names
    val groupCityOverall = group_city_data
      .map(city => city.getString(0)).distinct() // Get city name from Cassandrarow and remove duplicate cities
    val groupCityOverallCount = groupCityOverall.count() // Calculate overall count
    println("Total Count of distinct city names is: " + groupCityOverallCount)

    /// No.of observations for each city
    // Map to (group_city, 1)
    val group_city_count = group_city_data
      .map(city => (city.getString(0), 1))

    /// Count of each group city. ex: New York - 5059, San Fransisco - 3125
    // Calculate the count for each city
    val groupCityCounts = group_city_count.reduceByKey(_ + _)
    //Sort by value in descending order
    val groupCityCountsOverall = ListMap(groupCityCounts.collect.toSeq.sortWith(_._2 > _._2):_*)
    println("Count of observations for top 10 cities: ")
    groupCityCountsOverall.take(10).foreach(println) //Print top 10 cities

    /// Total count of unique group countries
    val groupCountrydata = rsvp.select("group_country")
    groupCountrydata.cache()
    val groupCountriesOverall = groupCountrydata
      .map(country => country.getString(0)).distinct()
    val groupCountriesOverallCount = groupCountriesOverall.count()
    println("Total count of disctinct country names: " + groupCountriesOverallCount)
    // Map to (group_country, 1)
    val groupCountryCount = groupCountrydata
      .map(country => (country.getString(0), 1))
      .reduceByKey(_ + _)

    val groupCountryCountsOverall = ListMap(groupCountryCount.collect.toSeq.sortWith(_._2 > _._2):_*)
    println("Count of observations for top 10 countries : ")
    groupCountryCountsOverall.take(10).foreach(println)

    // Counts of responses - yes or no
    val response = rsvp.select("response")
    response.cache()
    val responseCount = response
      .map(resp => (resp.getString(0), 1))
      .reduceByKey(_ + _)
    val responseOverallCount = ListMap(responseCount.collect.toSeq.sortWith(_._2 > _._2):_*)
    println("Breakup of yes and no responses: ")
    responseOverallCount.foreach(println)
    val yesCount = responseOverallCount("yes")
    val noCount = responseOverallCount("no")
    val totalCount = yesCount + noCount

    // Calculate yes & no proportions
    val yesProportion = (yesCount * 100) / totalCount
    val noProportion = (noCount * 100) / totalCount

    println("Proportion of people who answered yes: " + yesProportion + "%")
    println("Proportion of people who answered no: " + noProportion + "%")

    // response per country
    val responseCountry = rsvp.select("group_country", "response")
    val groupedResponses = responseCountry
      .map(line => (line.getString(0), line.getString(1)))
      .countByValue
      .map(line => (line._1._1, (line._1._2, line._2)))


    groupedResponses.take(10).foreach(println)


    // Users from which countries invite guests with them
    val countryGuests = rsvp.select("group_country", "guests")
      .map(line => (line.getString(0), line.getString(1).toInt))
      .filter(line => line._2 > 0)
      .reduceByKey(_ + _)

    // Top 10 countries in descending order of the no.of people who invited guests
    val topCountrieswithGuests = ListMap(countryGuests.collect.toSeq.sortWith(_._2 > _._2):_*)
    println("TOp 10 countries grouped by no. of people who invited guests: ")
    topCountrieswithGuests.take(10).foreach(println)


    // Users from which cities invite guests with them
    val cityGuests = rsvp.select("group_city", "guests")
      .map(line => (line.getString(0), line.getString(1).toInt))
      .filter(line => line._2 > 0)
      .reduceByKey(_ + _)

    // Top 10 cities in descending order of the no.of people who invited guests
    val topCitywithGuests = ListMap(cityGuests.collect.toSeq.sortWith(_._2 > _._2):_*)
    println("Top 10 cities grouped by no. of people who invited guests: ")
    topCitywithGuests.take(10).foreach(println)
    case class MemberResp(event_name: String, member_id: Long, response:Int)

    val eventNamesMemberResponses = rsvp.select("event_name","member_id","response")
      .map(line => MemberResp(line.getString(0).replace(',',';'), line.getString(1).toLong, if (line.getString(2).equals("yes")) 1 else 0))



  }
}
