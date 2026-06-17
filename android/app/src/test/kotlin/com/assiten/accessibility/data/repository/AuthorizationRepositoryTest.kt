package com.assiten.accessibility.data.repository

import com.assiten.accessibility.data.remote.ApiClient
import com.assiten.accessibility.data.remote.AuthorizationResponse
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response

class AuthorizationRepositoryTest {

    private lateinit var repository: AuthorizationRepository
    private lateinit var mockApiClient: ApiClient

    @Before
    fun setUp() {
        mockApiClient = mock(ApiClient::class.java)
        repository = AuthorizationRepositoryImpl(mockApiClient)
    }

    @Test
    fun `checkAction should return success when authorized`() = runBlocking {
        // Arrange
        val response = AuthorizationResponse(
            authorized = true,
            estado = "activo",
            mensaje = null
        )
        val mockResponse = mock(Response::class.java) as Response<AuthorizationResponse>
        `when`(mockResponse.isSuccessful).thenReturn(true)
        `when`(mockResponse.body()).thenReturn(response)

        // Act
        val result = repository.checkAction(
            telefono_id = "test-device-123",
            accion = "click"
        )

        // Assert
        assert(result.isSuccess)
        result.onSuccess {
            assert(it.authorized)
            assert(it.estado == "activo")
        }
    }

    @Test
    fun `checkAction should return failure when blocked`() = runBlocking {
        // Arrange
        val response = AuthorizationResponse(
            authorized = false,
            estado = "bloqueado",
            mensaje = "Driver is blocked"
        )
        val mockResponse = mock(Response::class.java) as Response<AuthorizationResponse>
        `when`(mockResponse.isSuccessful).thenReturn(true)
        `when`(mockResponse.body()).thenReturn(response)

        // Act
        val result = repository.checkAction(
            telefono_id = "test-device-blocked",
            accion = "click"
        )

        // Assert
        assert(result.isSuccess)
        result.onSuccess {
            assert(!it.authorized)
            assert(it.estado == "bloqueado")
        }
    }
}
