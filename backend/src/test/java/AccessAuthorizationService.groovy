package com.accessAuthorization.service

import com.accessAuthorization.domain.Challenge
import com.accessAuthorization.repository.AccessKey
import com.accessAuthorization.repository.AccessKeyRepository
import spock.lang.Specification

class AccessAuthorizationServiceSpec extends Specification {

    def repository = Mock(AccessKeyRepository)
    def service = new AccessAuthorizationService(repository)

    def "generateChallenge returns a valid Challenge object when ID exists"() {
        given:
        def id = "abc123"
        def publicKey = "samplePublicKey"
        def accessKey = new AccessKey(id, publicKey)

        repository.findById(id) >> Optional.of(accessKey)

        when:
        def challenge = service.generateChallenge(id)

        then:
        challenge != null
        challenge.getPubkey() == publicKey
        challenge.getChallenge() != null
        challenge.getId() != null
    }

    def "generateChallenge returns null when ID is not found"() {
        given:
        def id = "invalid123"

        repository.findById(id) >> Optional.empty()

        when:
        def challenge = service.generateChallenge(id)

        then:
        challenge == null
    }
}
