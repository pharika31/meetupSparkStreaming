import org.json4s._

case class GroupDetails(group_city: String, group_country: String, group_id: Int, group_name: String, group_lon: Int, group_lat: Int, group_state: Option[String]=None,
                        group_topics: JArray)
