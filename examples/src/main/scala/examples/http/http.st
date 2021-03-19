S_http=?Request(msg: RequestLine).rec X.(&{
	?AcceptEncodings(msg: String).X,
	?Accept(msg: String).X,
	?DoNotTrack(msg: Boolean).X,
	?UpgradeIR(msg: Boolean).X,
	?Connection(msg: String).X,
	?UserAgent(msg: String).X,
	?AcceptLanguage(msg: String).X,
	?Host(msg: String).X,
	?RequestBody(msg: Body).!HttpVersion(msg: Version).+{
		!Code404(msg: String).rec Y.(+{
			!ETag(msg: String).Y,
			!Server(msg: String).Y,
			!ContentLength(msg: Int).Y,
			!ContentType(msg: String).Y,
			!Vary(msg: String).Y,
			!Via(msg: String).Y,
			!StrictTS(msg: String).Y,
			!ResponseBody(msg: Body).end,
			!AcceptRanges(msg: String).Y,
			!LastModified(msg: ZonedDateTime).Y,
			!Date(msg: ZonedDateTime).Y
		}),
		!Code200(msg: String).rec Z.(+{
			!ETag(msg: String).Z,
			!Server(msg: String).Z,
			!ContentLength(msg: Int).Z,
			!ContentType(msg: String).Z,
			!Vary(msg: String).Z,
			!Via(msg: String).Z,
			!StrictTS(msg: String).Z,
			!ResponseBody(msg: Body).end,
			!AcceptRanges(msg: String).Z,
			!LastModified(msg: ZonedDateTime).Z,
			!Date(msg: ZonedDateTime).Z
		})
	}
})





