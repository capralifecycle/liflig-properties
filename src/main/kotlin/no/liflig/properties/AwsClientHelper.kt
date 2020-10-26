package no.liflig.properties

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.DecryptionFailureException
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse
import software.amazon.awssdk.services.secretsmanager.model.InternalServiceErrorException
import software.amazon.awssdk.services.secretsmanager.model.InvalidParameterException
import software.amazon.awssdk.services.secretsmanager.model.InvalidRequestException
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest
import software.amazon.awssdk.services.ssm.model.InternalServerErrorException
import software.amazon.awssdk.services.ssm.model.InvalidKeyIdException
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException
import software.amazon.awssdk.services.ssm.model.ParameterVersionNotFoundException

object AwsClientHelper {
    private val systemsManagement = SsmClient.builder()
        .build()
    private val secretsManager = SecretsManagerClient.builder()
        .build()

    fun getParametersByPath(path: String): Map<String, String> {
        val parameters = mutableMapOf<String, String>()

        val request = GetParametersByPathRequest.builder()
            .path(path)
            .withDecryption(true)
            .recursive(true)
            .build()

        try {
            val response = systemsManagement.getParametersByPathPaginator(request)
            response.forEach { responses ->
                responses.parameters().forEach { parameter ->
                    parameters[parameter.name()] = parameter.value()
                }
            }
        } catch (e: InternalServerErrorException) {
            // An error occurred on the server side.
            throw e
        } catch (e: InvalidKeyIdException) {
            // The query key ID is not valid.
            throw e
        } catch (e: ParameterNotFoundException) {
            // The parameter could not be found. Verify the name and try again.
            throw e
        } catch (e: ParameterVersionNotFoundException) {
            // The specified parameter version was not found. Verify the parameter name and version, and try again.
            throw e
        }

        return parameters
    }

    fun getSecret(path: String): String {
        val response: GetSecretValueResponse

        val request = GetSecretValueRequest.builder()
            .secretId(path)
            .build()

        try {
            response = secretsManager.getSecretValue(request)
        } catch (e: ResourceNotFoundException) {
            // We can't find the resource that you asked for.
            throw e
        } catch (e: InvalidParameterException) {
            // You provided an invalid value for a parameter.
            throw e
        } catch (e: InvalidRequestException) {
            // You provided a parameter value that is not valid for the current state of the resource.
            // Possible causes:
            //   You tried to perform the operation on a secret that's currently marked deleted.
            //   You tried to enable rotation on a secret that doesn't already have a
            //     Lambda function ARN configured and you didn't include such an ARN as a parameter in this call.
            throw e
        } catch (e: DecryptionFailureException) {
            // Secrets Manager can't decrypt the protected secret text using the provided KMS key.
            throw e
        } catch (e: InternalServiceErrorException) {
            // An error occurred on the server side
            throw e
        }

        return response.secretString()
    }
}
