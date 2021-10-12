S_smtp = !M220(msg: String)[*].&{
	?Helo(hostname: String)[*].!M250(msg: String)[*].rec X.(&{
		?MailFrom(addr: String)[0.5].!M250(msg: String)[*].rec Y.(&{
			?RcptTo(addr: String)[0.3].!M250(msg: String)[*].Y,
			?Data()[0.25].!M354(msg: String)[*].?Content(txt: String)[*].!M250(msg: String)[*].X,
			?Quit()[*].!M221(msg: String)[*]}),
		?Quit()[*].!M221(msg: String)[*]}),
	?Quit()[*].!M221(msg: String)[*] }