package com.locoquest.app

import com.locoquest.app.dao.BenchmarkDatabase
import com.locoquest.app.dto.User

class AppModule {
    companion object{
        var db: BenchmarkDatabase? = null
        var user: User = User("0", "", ArrayList())
    }
}