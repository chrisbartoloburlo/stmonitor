S_auth=rec Y.(!Auth(uname: String, pwd: String)[util.validateUname(uname)].&{
	?Succ(origTok: String)[util.validateTok(origTok, uname)].rec X.(+{
		!Get(resource: String, reqTok: String).&{
			?Res(content: String)[origTok==reqTok].X,
			?Timeout().Y
		},
		!Rvk(rvkTok: String)[origTok==rvkTok].end}),
	?Fail(code: Int).end
})