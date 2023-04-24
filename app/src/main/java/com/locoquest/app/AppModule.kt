package com.locoquest.app

import com.locoquest.app.dao.DB
import com.locoquest.app.dto.User

class AppModule {
    companion object{
        var db: DB? = null
        val guest: User = User("0", "Guest", HashMap())
        var user: User = guest
    }
}