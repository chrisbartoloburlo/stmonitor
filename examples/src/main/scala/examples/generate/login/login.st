S_login=rec X.( ?Login(uname: String, pwd: String, token: String)[util.validateAuth(uname, token)].+{!Success(id: String)[util.validateId(id,uname)], !Retry().X} )