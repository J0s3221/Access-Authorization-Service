package com.accessauthorization.service

import com.accessauthorization.domain.Challenge
import com.accessauthorization.repository.AccessKey
import com.accessauthorization.repository.AccessKeyRepository
import com.accessauthorization.service.AccessAuthorizationService
import spock.lang.Specification
import spock.lang.Unroll

class AccessAuthorizationServiceTest extends Specification {

    def repository = Mock(AccessKeyRepository)
    def service = new AccessAuthorizationService(repository)

    def "generateChallenge returns a valid Challenge object when ID exists"() {
        given: "An existing access key"
        def id = "abc123"
        def publicKey = "samplePublicKey"
        def accessKey = new AccessKey(id: id, publicKey: publicKey)

        and: "The repository will return this access key"
        repository.findById(id) >> Optional.of(accessKey)

        when: "Generating a challenge"
        def challenge = service.generateChallenge(id)

        then: "The challenge should be properly formed"
        challenge != null
        challenge.pubkey == publicKey
        challenge.challenge != null
        !challenge.challenge.isEmpty()
        challenge.id == id
    }

    def "generateChallenge returns null when ID is not found"() {
        given: "A non-existent ID"
        def id = "invalid123"

        and: "The repository returns empty"
        repository.findById(id) >> Optional.empty()

        when: "Generating a challenge"
        def challenge = service.generateChallenge(id)

        then: "No challenge should be returned"
        challenge == null
    }

    @Unroll
    def "generateChallenge handles edge cases for ID: #id"(String id, boolean shouldExist) {
        given:
        repository.findById(id) >> (shouldExist ? 
            Optional.of(new AccessKey(id: id, publicKey: "key")) : 
            Optional.empty())

        expect:
        (service.generateChallenge(id) != null) == shouldExist

        where:
        id          | shouldExist
        null        | false
        ""          | false
        "   "       | false
        "valid123"  | true
    }
}