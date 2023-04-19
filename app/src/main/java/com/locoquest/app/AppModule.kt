package com.locoquest.app

import com.locoquest.app.dao.BenchmarkDatabase
import com.locoquest.app.dto.User

class AppModule {
    companion object{
        var db: BenchmarkDatabase? = null
        val guest: User = User("0", "Guest", HashMap())
        var user: User = guest
    }
}