S_http=!Request(msg: RequestLine).rec X.(+{
	!AcceptEncodings(msg: String).X,
	!Accept(msg: String).X,
	!DoNotTrack(msg: Boolean).X,
	!UpgradeIR(msg: Boolean).X,
	!Connection(msg: String).X,
	!UserAgent(msg: String).X,
	!AcceptLanguage(msg: String).X,
	!Host(msg: String).X,
	!RequestBody(msg: Body).?HttpVersion(msg: Version).&{
		?Code404(msg: String).rec Y.(&{
			?ETag(msg: String).Y,
			?Server(msg: String).Y,
			?ContentLength(msg: Int).Y,
			?ContentType(msg: String).Y,
			?Vary(msg: String).Y,
			?Via(msg: String).Y,
			?StrictTS(msg: String).Y,
			?ResponseBody(msg: Body).end,
			?AcceptRanges(msg: String).Y,
			?LastModified(msg: ZonedDateTime).Y,
			?Date(msg: ZonedDateTime).Y
		}),
		?Code200(msg: String).rec Z.(&{
			?ETag2(msg: String).Z,
			?Server2(msg: String).Z,
			?ContentLength2(msg: Int).Z,
			?ContentType2(msg: String).Z,
			?Vary2(msg: String).Z,
			?Via2(msg: String).Z,
			?StrictTS2(msg: String).Z,
			?ResponseBody2(msg: Body).end,
			?AcceptRanges2(msg: String).Z,
			?LastModified2(msg: ZonedDateTime).Z,
			?Date2(msg: ZonedDateTime).Z
		})
	}
})
