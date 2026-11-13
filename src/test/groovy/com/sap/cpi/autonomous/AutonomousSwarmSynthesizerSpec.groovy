package com.sap.cpi.autonomous

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import spock.lang.Specification

class AutonomousSwarmSynthesizerSpec extends Specification {

    AutonomousSwarmSynthesizer synthesizer = new AutonomousSwarmSynthesizer()
    Message message = Mock(Message)

    def "Should autonomously synthesize ISA-M compliant iFlow and trigger Zero-Touch Piper deployment"() {
        given: "An emergency Ariba supplier disruption event with HTTPS OpenAPI endpoint"
        def disruptionEvent = """
        {
            "disruptionId": "ARIBA-DISRUPT-2031-0042",
            "aribaSupplierId": "ACM-SUP-998120",
            "supplierName": "Nordic BioMaterials AB",
            "openApiEndpointUrl": "[https://api.nordicbiomaterials.se/b2b/v2/purchase-orders](https://api.nordicbiomaterials.se/b2b/v2/purchase-orders)"
        }
        """
        message.getBody(String.class) >> disruptionEvent

        when: "The AI Swarm orchestrator executes"
        synthesizer.processData(message)

        then: "BPMN2 iFlow artifact is synthesized and approved for Zero-Touch CI/CD"
        1 * message.setHeader("X-Swarm-Synthesis-ID", { it.startsWith("SYN-") })
        1 * message.setHeader("X-Swarm-Generated-Artifact", "Ariba_Emergency_Nordic_BioMaterials_AB_B2B_v1")
        1 * message.setHeader("X-Swarm-ISAM-Pattern", "Cloud-to-Cloud | B2B Event-Driven Procurement Sync")
        1 * message.setHeader("X-Swarm-Governance-Status", "APPROVED_FOR_ZERO_TOUCH_CICD")
        1 * message.setHeader("CamelHttpResponseCode", 201)
        1 * message.setBody(_ as String) >> { args ->
            def parsed = new JsonSlurper().parseText(args[0])
            assert parsed.autonomousEnterpriseSynthesis.zeroTouchCicdPipeline.projectPiperDeploy == "DEPLOYED_TO_BTP_RUNTIME"
            assert parsed.autonomousEnterpriseSynthesis.zeroTouchCicdPipeline.executiveNotification == "DISPATCHED_TO_CEO_AND_CSCO"
        }
    }

    def "Should quarantine synthesized iFlow when SonarQube security gate detects non-TLS HTTP endpoint"() {
        given: "An Ariba event pointing to an insecure HTTP endpoint"
        def insecureEvent = """
        {
            "disruptionId": "ARIBA-DISRUPT-999",
            "aribaSupplierId": "SUP-UNSAFE-01",
            "supplierName": "Legacy Vendor",
            "openApiEndpointUrl": "[http://insecure-vendor.example.com/api](http://insecure-vendor.example.com/api)"
        }
        """
        message.getBody(String.class) >> insecureEvent

        when: "The SonarQube security auditor evaluates the endpoint"
        synthesizer.processData(message)

        then: "Deployment is blocked with HTTP 422 quarantine status"
        1 * message.setHeader("X-Swarm-Governance-Status", "QUARANTINED_POLICY_VIOLATION")
        1 * message.setHeader("CamelHttpResponseCode", 422)
    }
}
