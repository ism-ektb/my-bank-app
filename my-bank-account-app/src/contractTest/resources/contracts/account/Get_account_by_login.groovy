package contracts.account

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description 'Get account for login testUser'
    name 'get_account_by_login'

    request {
        method GET()
        url '/account/testUser'
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
        body(
                name: 'testUser',
                birthdate: 1999-01-01,
                login: 'test-user',
                ballance: 10
        )
    }
}
