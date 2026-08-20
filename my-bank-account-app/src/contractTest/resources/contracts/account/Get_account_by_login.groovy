package contracts.account

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description 'Get all accounts for transfer menu'
    name 'get_all_accounts'

    request {
        method GET()
        url '/account/all'
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
        body("name":"testUser1","login":"testUser1")
    }
}