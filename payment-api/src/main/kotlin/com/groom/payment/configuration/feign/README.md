# Feign Client 설정 가이드

Customer API에서 Store Service와 통신하기 위한 Feign Client 설정입니다.

## 구성 요소

### 1. FeignClientProperties
`@ConfigurationProperties`를 사용한 프로퍼티 관리

```kotlin
@ConfigurationProperties(prefix = "feign.clients")
data class FeignClientProperties(
    val storeService: ServiceConfig = ServiceConfig()
)
```

### 2. StoreFeignClient
Store Service와 통신하는 Feign Client 인터페이스

```kotlin
@FeignClient(
    name = "store-service",
    url = "\${feign.clients.store-service.url:http://localhost:8081}"
)
interface StoreFeignClient : StoreClient
```

### 3. StoreClientAdapter
도메인 로직과 외부 통신을 분리하는 Adapter

```kotlin
@Component
class StoreClientAdapter(
    private val storeFeignClient: StoreFeignClient
)
```

## 설정 방법

### application.yml
```yaml
feign:
  clients:
    store-service:
      url: http://localhost:8081
      connect-timeout: 5000
      read-timeout: 5000
      logger-level: basic
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: basic
```

### application-local.yml (개발 환경)
```yaml
feign:
  clients:
    store-service:
      url: http://localhost:8081
      connect-timeout: 10000
      read-timeout: 10000
      logger-level: full  # 상세 로깅
```

### application-prod.yml (프로덕션 환경)
```yaml
feign:
  clients:
    store-service:
      url: ${STORE_SERVICE_URL:http://store-service:8081}  # 환경변수 사용
      connect-timeout: 3000
      read-timeout: 3000
      logger-level: basic
```

## 사용 예시

### Service에서 사용
```kotlin
@Service
class CustomerService(
    private val storeClientAdapter: StoreClientAdapter
) {
    fun getStoreInfo(storeId: Long): StoreResponse? {
        return storeClientAdapter.getStore(storeId)
    }

    fun checkStoreExists(storeId: Long): Boolean {
        return storeClientAdapter.existsStore(storeId)
    }
}
```

### 예외 처리
```kotlin
try {
    val store = storeClientAdapter.getStore(storeId)
} catch (e: FeignClientException.NotFound) {
    // 404 처리
} catch (e: FeignClientException.ServiceUnavailable) {
    // 503 처리
} catch (e: FeignClientException) {
    // 기타 에러 처리
}
```

## 주요 특징

1. **@ConfigurationProperties 활용**
   - 프로퍼티 구조화 및 타입 안전성
   - IDE 자동완성 지원

2. **SpEL 표현식으로 URL 주입**
   - `url = "\${feign.clients.store-service.url}"`
   - 환경별 설정 변경 용이

3. **에러 처리**
   - 커스텀 ErrorDecoder로 HTTP 상태별 예외 변환
   - FeignClientException 계층 구조

4. **Adapter 패턴**
   - 도메인 로직과 외부 통신 분리
   - 테스트 용이성 향상

## 테스트

### Unit Test
```kotlin
@UnitTest
class MyServiceTest {
    @MockBean
    private lateinit var storeFeignClient: StoreFeignClient

    @Test
    fun `should get store info`() {
        given(storeFeignClient.getStore(1L))
            .willReturn(StoreResponse(...))

        // 테스트 로직
    }
}
```

### Integration Test
```kotlin
@IntegrationTest
@AutoConfigureMockMvc
class StoreIntegrationTest {
    @MockBean
    private lateinit var storeFeignClient: StoreFeignClient

    @Test
    fun `should integrate with store service`() {
        // 통합 테스트 로직
    }
}
```

## 환경변수 설정 (프로덕션)

Kubernetes나 Docker Compose 환경에서:

```yaml
# docker-compose.yml
environment:
  STORE_SERVICE_URL: http://store-service:8081
```

```yaml
# kubernetes deployment
env:
  - name: STORE_SERVICE_URL
    value: "http://store-service.default.svc.cluster.local:8081"
```
