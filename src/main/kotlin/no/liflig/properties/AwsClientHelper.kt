package no.liflig.properties

import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(AwsClientHelper::class.java)
    private val systemsManagement = SsmClient.builder()
        .build()
    private val secretsManager = SecretsManagerClient.builder()
        .build()

    @Throws(ParameterLoadingException::class)
    fun getParametersByPath(path: String): Map<String, String> {
        logger.debug("Loading parameters at path {}", path)
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
            throw ParameterLoadingException.InternalErrorException(
                path,
                "An error occurred on the server side.",
                e
            )
        } catch (e: InvalidKeyIdException) {
            throw ParameterLoadingException.InvalidParameterException(
                path,
                "The query key ID is not valid.",
                e
            )
        } catch (e: ParameterNotFoundException) {
            throw ParameterLoadingException.InvalidParameterException(
                path,
                "The parameter could not be found. Verify the name and try again.",
                e
            )
        } catch (e: ParameterVersionNotFoundException) {
            throw ParameterLoadingException.InvalidParameterException(
                path,
                "The specified parameter version was not found. " +
                    "Verify the parameter name and version, and try again.",
                e
            )
        }

        return parameters
    }

    @Throws(SecretLoadingException::class)
    fun getSecret(path: String): String {
        logger.debug("Loading secret at path {}", path)
        val response: GetSecretValueResponse

        val request = GetSecretValueRequest.builder()
            .secretId(path)
            .build()

        try {
            response = secretsManager.getSecretValue(request)
        } catch (e: ResourceNotFoundException) {
            throw SecretLoadingException.InvalidSecretException(
                path,
                "We can't find the resource that you asked for.",
                e
            )
        } catch (e: InvalidParameterException) {
            throw SecretLoadingException.InvalidSecretException(
                path,
                "You provided an invalid value for a parameter.",
                e
            )
        } catch (e: InvalidRequestException) {
            throw SecretLoadingException.InvalidSecretException(
                path,
                """You provided a parameter value that is not valid for the current state of the resource. Possible causes:
                    |You tried to perform the operation on a secret that's currently marked deleted.
                    |You tried to enable rotation on a secret that doesn't already have a
                    |Lambda function ARN configured and you didn't include such an ARN as a parameter in this call.""".trimMargin(),
                e
            )
        } catch (e: DecryptionFailureException) {
            throw SecretLoadingException.DecryptionException(
                path,
                "Secrets Manager can't decrypt the protected secret text using the provided KMS key.",
                e
            )
        } catch (e: InternalServiceErrorException) {
            throw SecretLoadingException.InternalErrorException(
                path,
                "An error occurred on the server side",
                e
            )
        }

        return response.secretString()
    }
}

sealed class ParameterLoadingException(
    message: String,
    cause: Throwable?
) : PropertyLoadingException(message, cause) {
    data class InvalidParameterException(
        val parameter: String,
        override val message: String,
        override val cause: Throwable?
    ) : ParameterLoadingException(message, cause)

    data class InternalErrorException(
        val parameter: String,
        override val message: String,
        override val cause: Throwable?
    ) : ParameterLoadingException(message, cause)
}

sealed class SecretLoadingException(
    message: String,
    cause: Throwable?
) : PropertyLoadingException(message, cause) {
    data class InvalidSecretException(
        val secretPath: String,
        override val message: String,
        override val cause: Throwable?
    ) : SecretLoadingException(message, cause)

    data class DecryptionException(
        val secretPath: String,
        override val message: String,
        override val cause: Throwable?
    ) : SecretLoadingException(message, cause)

    data class InternalErrorException(
        val secretPath: String,
        override val message: String,
        override val cause: Throwable?
    ) : SecretLoadingException(message, cause)
}
