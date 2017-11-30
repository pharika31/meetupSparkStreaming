case class RSVPData(
                     venue: Option[VenueDetails] = None,
                     visibility: String,
                     response: String,
                     guests: Int,
                     member: MemberDetails,
                     rsvp_id: Long,
                     mtime: Long,
                     event: EventDetails,
                     group: GroupDetails
                   )