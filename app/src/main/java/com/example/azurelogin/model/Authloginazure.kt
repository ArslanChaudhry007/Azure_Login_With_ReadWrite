package com.example.azurelogin.model

data class Authloginazure(
    var account_mode: String = "",
    var authorities: List<Authority>,
    var authorization_user_agent: String = "",
    var client_id: String = "",
    var redirect_uri: String = ""
) {
    data class Authority(
        var audience: Audience,
        var type: String = ""
    )

    data class Audience(
        var tenant_id: String = "",
        var type: String = ""
    )

}