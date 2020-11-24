S_auth=rec Y.( ?Auth(uname: String, pwd: String)[util.validateUname(uname)].+{!Succ(tok: String)[util.validateTok(tok, uname)], !Fail(Code: Int).Y} )
