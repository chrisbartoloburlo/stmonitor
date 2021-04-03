S_auth=rec Y.(!Auth(uname: String, pwd: String).&{
	?Succ(tok: String).rec X.(+{
		!Get(resource: String, tok: String).&{
			?Res(content: String).X,
			?Timeout().Y
		},
		!Rvk(tok: String).end}),
	?Fail(code: Int).end
})