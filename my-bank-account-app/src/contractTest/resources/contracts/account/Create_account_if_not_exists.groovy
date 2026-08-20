package contracts.account

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description 'Create account if do not exits'
    name 'create_account_if_not_exist'

    request {
        method POST()
        url '/account'
        headers {
            header 'Authorization', value(
                    // Для консьюмера (WireMock): любой Bearer-токен
                    consumer(regex('Bearer\\s+.+')),
                    // Для провайдера (MockMvc-тест): ровно этот токен
                    producer('Bearer test-token')
            )
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body("name":"testUser", "birthdate":[1999,1,1], "login":"testUser", "balance":10)
    }
}
