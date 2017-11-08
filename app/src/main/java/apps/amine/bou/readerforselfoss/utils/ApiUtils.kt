package apps.amine.bou.readerforselfoss.utils

import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import retrofit2.Response

fun Response<SuccessResponse>.succeeded(): Boolean =
    this.code() === 200 && this.body() != null && this.body()!!.isSuccess