package com.locoquest.app

import com.locoquest.app.dao.DB
import com.locoquest.app.dto.User

class AppModule {
    companion object{
        var db: DB? = null
        val guest: User = User("0","Guest")
        var user: User = guest
        const val DEFAULT_REACH = 150.0
        const val BOOSTED_REACH = 250.0
        const val BOOSTED_DURATION = 300
        const val DEBUG = false
        //const val AD_FREE = false
    }
}