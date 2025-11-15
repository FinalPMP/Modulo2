# Módulo 2 – Serviço de Login com JWT e API Gateway (RetroCore)

Este repositório implementa o **Módulo 2** do projeto RetroCore:  
uma solução de **autenticação** baseada em:

- **Spring Boot 3** e **Java 21**
- **Spring Security** e **BCrypt**
- **JWT** usando a biblioteca `java-jwt` (Auth0)
- **PostgreSQL** para persistência de usuários
- **Spring Cloud Gateway** como API Gateway

Os objetivos deste módulo são:

- Centralizar o **registro** e **login** de usuários.
- Emitir um **token JWT** com informações do usuário (username e role).
- Expor a autenticação por meio de um **API Gateway**, desacoplando clientes dos detalhes internos.
- Manter a separação entre **Domínio**, **Aplicação / Infraestrutura** e **Borda (Gateway)** seguindo **Clean Architecture** e **SOLID**.

---

## 1. Visão Geral da Arquitetura

### 1.1 Estrutura de módulos Maven

O projeto é um **multi-módulo Maven** definido em `pom.xml` raiz:

- `login-domain` – Núcleo de domínio de autenticação.
- `login-spring` – Serviço HTTP de login/registro.
- `gateway` – API Gateway com Spring Cloud Gateway.

```xml
<modules>
    <module>login-domain</module>
    <module>login-spring</module>
    <module>gateway</module>
</modules>
````

---

### 1.2 Módulo `login-domain` (Domínio)

Caminho: `login-domain/src/main/java/com/retrocore/mod2/login/domain`

Responsável pelas **regras de negócio de autenticação**, expressas em termos de modelo, portas e casos de uso.

Principais componentes:

* `User.java`
  Modelo de usuário da aplicação, com:

  * `id`
  * `username`
  * `password` (hash)
  * `fullName`
  * `role` (ex.: `"USER"`)

* `UserRepositoryPort.java`
  Interface (porta de saída) que define como o domínio enxerga o repositório de usuários:

  * `Optional<User> findByUsername(String username)`
  * `boolean existsByUsername(String username)`
  * `User save(User user)`

* `exception/`

  * `InvalidCredentialsException`
    Lançada quando usuário/senha são inválidos ou quando há conflito de credenciais.
  * `UserNotFoundException`
    (Disponível para cenários onde seja necessário tratar ausência explícita de usuário.)

* `service/`

  * `AuthenticateUserUseCase`
    Caso de uso de autenticação:

    * Busca usuário por `username`.
    * Compara a senha em texto plano usando `PasswordEncoder`.
    * Lança `InvalidCredentialsException` se não houver match.
  * `RegisterUserUseCase`
    Caso de uso de registro:

    * Verifica se o `username` já existe (`existsByUsername`).
    * Caso exista, lança `InvalidCredentialsException` (“Usuário já existe.”).
    * Cria um novo `User` com senha criptografada (`BCrypt`) e role `"USER"`.
    * Persiste o usuário via `UserRepositoryPort`.

Mesmo utilizando `PasswordEncoder` (do Spring Security), a modelagem do módulo enfatiza o conceito de **porta de repositório** e **casos de uso** independentes de detalhes HTTP.

---

### 1.3 Módulo `login-spring` (Aplicação / Infraestrutura)

Caminho: `login-spring/src/main/java/com/retrocore/mod2/login/`

Responsável por expor o domínio via HTTP, implementar a porta de repositório e orquestrar autenticação + emissão de token.

Principais pacotes:

* **Aplicação**

  * `RetrocoreLoginApplication.java`
    Classe principal do Spring Boot.

  * `application/`

    * `LoginService`
      Orquestra o fluxo de login:

      * Chama `AuthenticateUserUseCase`.
      * Gera token JWT usando `JwtService`.
      * Retorna `LoginResponse` (username + token).
    * `RegisterService`
      Orquestra o fluxo de registro:

      * Chama `RegisterUserUseCase`.
      * Gera token JWT para o novo usuário.
      * Retorna `RegisterResponse` (id + username + token).

* **API**

  * `api/AuthController.java`
    Controller REST com base path `/api/v1/auth`:

    * `POST /login`
    * `POST /register`
  * `api/dto/`

    * `LoginRequest` (`username`, `password`) com validação.
    * `LoginResponse` (`username`, `token`).
    * `RegisterRequest` (`username`, `password`, `fullName`) com validação.
    * `RegisterResponse` (`id`, `username`, `token`).
  * `GlobalExceptionHandler`
    `@RestControllerAdvice` que trata:

    * `InvalidCredentialsException` → HTTP 401
    * `UserNotFoundException` → HTTP 404
    * `MethodArgumentNotValidException` → HTTP 400 (erros de validação)
    * `Exception` genérica → HTTP 500
      Sempre retorna um corpo padronizado com:
    * `timestamp`
    * `status`
    * `error`
    * `message`
    * (Opcional) `errors` com detalhes de campos inválidos.

* **Infraestrutura**

  * `infrastructure/config/BeansConfig.java`
    Registra os casos de uso como beans Spring:

    * `AuthenticateUserUseCase`
    * `RegisterUserUseCase`
      Injetando `UserRepositoryPort` e `PasswordEncoder`.

  * `infrastructure/config/SecurityConfig.java`
    Configuração de segurança:

    * Desabilita CSRF.
    * Libera acesso para `/api/v1/auth/**`.
    * Exige autenticação para demais endpoints.
    * Configura `BCryptPasswordEncoder` como `PasswordEncoder`.

  * `infrastructure/entity/UserEntity.java`
    Entidade JPA mapeada para a tabela `users`.

  * `infrastructure/repository/JpaUserRepository.java`
    Repositório JPA com:

    * `findByUsername`
    * `existsByUsername`

  * `infrastructure/repository/UserRepositoryAdapter.java`
    Adapter que implementa `UserRepositoryPort`, convertendo:

    * `UserEntity` ↔ `User`
      usando `UserMapper`.

  * `infrastructure/security/JwtService.java`
    Serviço responsável por emitir tokens JWT:

    * Usa `Algorithm.HMAC256(secret)`.
    * Claims:

      * `sub` → username
      * `role` → papel do usuário
      * `iat`, `exp` conforme `expiration-minutes`.
    * Propriedades externas:

      * `jwt.secret`
      * `jwt.expiration-minutes`

* **Mapper**

  * `mapper/UserMapper.java`
    Mapeia entre `User` (domínio) e `UserEntity` (JPA).

* **Configuração**

  * `src/main/resources/application.yml`
    Define:

    * Porta da aplicação (`8080`).
    * Propriedades do datasource PostgreSQL.
    * Propriedades JPA.
    * Propriedades do JWT (secret e expiration).

---

### 1.4 Módulo `gateway` (API Gateway)

Caminho: `gateway/src/main/java/com/retrocore/mod2/gateway/`

Responsável por expor uma borda única de entrada para o módulo de autenticação.

Principais componentes:

* `GatewayApplication.java`
  Classe principal do Spring Boot Gateway.

* `config/RouteConfig.java`
  Configura as rotas do Spring Cloud Gateway:

  ```java
  @Bean
  public RouteLocator loginRoutes(RouteLocatorBuilder builder) {
      return builder.routes()
              .route("login-service", r -> r
                      .path("/api/v1/auth/**")
                      .uri("http://login-service:8080"))
              .build();
  }
  ```

  Ou seja:

  * Qualquer requisição que chegue ao **gateway** em `http://localhost:9100/api/v1/auth/**`
  * É roteada para o **login-service** no container (`login-service:8080`).

* `src/main/resources/application.yml`
  Define:

  * `server.port: 9100`
  * `spring.application.name: retrocore_mod2_gateway`

---

## 2. Clean Architecture na Prática

O módulo 2 também segue o espírito da **Clean Architecture**, organizando o código em camadas:

* **Domínio (`login-domain`)**

  * Modelo: `User`
  * Porta: `UserRepositoryPort`
  * Casos de uso: `AuthenticateUserUseCase`, `RegisterUserUseCase`
  * Exceções de domínio

* **Aplicação/Infraestrutura (`login-spring`)**

  * Controllers REST, DTOs, services de aplicação
  * Configuração de segurança, entidade JPA, repositórios, adaptação de repositório
  * Emissão de token JWT

* **Borda / Gateway (`gateway`)**

  * Serviço independente que conhece apenas os caminhos HTTP e o host lógico `login-service`.

Dependências seguem a direção de fora para dentro:

* Controller → Services → Casos de uso → Porta → Adapter → Repositório JPA → Banco
* Gateway → Login-service (via HTTP)
* Domínio não conhece HTTP, PostgreSQL, JWT ou Gateway; conhece apenas contratos abstratos.

---

## 3. Princípios SOLID Aplicados

Alguns exemplos de SOLID no módulo:

* **S – Single Responsibility Principle**

  * `AuthenticateUserUseCase` só trata autenticação.
  * `RegisterUserUseCase` só trata registro.
  * `JwtService` só se preocupa em gerar tokens.
  * `UserMapper` só faz mapeamento entre modelos.

* **O – Open/Closed Principle**

  * Novas formas de persistência (por exemplo, outro banco ou repositório em memória) podem ser adicionadas implementando `UserRepositoryPort`, sem modificar os casos de uso.

* **L – Liskov Substitution Principle**

  * Qualquer implementação de `UserRepositoryPort` (como `UserRepositoryAdapter`) pode ser usada no lugar da interface sem quebrar o comportamento esperado.

* **I – Interface Segregation Principle**

  * `UserRepositoryPort` expõe apenas operações necessárias para os casos de uso, evitando interfaces “inchadas”.

* **D – Dependency Inversion Principle**

  * Casos de uso dependem de abstrações (`UserRepositoryPort`), não de detalhes (`JpaUserRepository`).
  * A ligação é feita na borda via `BeansConfig`.

---

## 4. Endpoints da API de Autenticação

Base direta do serviço de login:
`http://localhost:8080/api/v1/auth`

Base via Gateway:
`http://localhost:9100/api/v1/auth`

### 4.1 Registro de usuário

**POST** `/api/v1/auth/register`

Request body (`RegisterRequest`):

```json
{
  "username": "bruno",
  "password": "123456",
  "fullName": "Bruno Rezende"
}
```

Restrições:

* `username` obrigatório.
* `password` obrigatório.
* `fullName` obrigatório.

Resposta `200 OK` (`RegisterResponse`):

```json
{
  "id": 1,
  "username": "bruno",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Possíveis erros:

* `400 Bad Request` – erro de validação de campos.
* `401 Unauthorized` – usuário já existe (tratado como credencial inválida).
* `500 Internal Server Error` – erro genérico.

---

### 4.2 Login do usuário

**POST** `/api/v1/auth/login`

Request body (`LoginRequest`):

```json
{
  "username": "bruno",
  "password": "123456"
}
```

Resposta `200 OK` (`LoginResponse`):

```json
{
  "username": "bruno",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Possíveis erros:

* `401 Unauthorized` – usuário ou senha inválidos.
* `400 Bad Request` – campos obrigatórios ausentes.
* `500 Internal Server Error` – erro genérico.

---

## 5. Execução do Projeto

### 5.1 Requisitos

* Docker e Docker Compose
* Java 21 (para rodar localmente sem Docker)
* Maven Wrapper incluído (`mvnw`, `mvnw.cmd`)

---

### 5.2 Executando com Docker Compose

Na raiz do projeto (`retrocore-modulo2`):

```bash
docker-compose up -d --build
```

Serviços definidos em `docker-compose.yml`:

* `login-service`

  * Build: `login-spring/Dockerfile`
  * Porta interna: `8080`
  * Conectado ao banco `mod2-db`
  * Recebe variáveis de ambiente para datasource e JWT (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`).

* `gateway`

  * Build: `gateway/Dockerfile`
  * Porta externa: `9100`
  * Encaminha `/api/v1/auth/**` para `login-service:8080`.

* `mod2-db`

  * Imagem: `postgres:16`
  * Banco: `retrocore_mod2`
  * Usuário: `postgres`
  * Senha: `123456`
  * Porta externa: `5440 -> 5432`.

Com os containers no ar, é possível testar:

* Diretamente no serviço de login:

  * `http://localhost:8080/api/v1/auth/register`
  * `http://localhost:8080/api/v1/auth/login`
* Via Gateway:

  * `http://localhost:9100/api/v1/auth/register`
  * `http://localhost:9100/api/v1/auth/login`

---

### 5.3 Execução local (sem Docker)

1. Subir um PostgreSQL local com:

   * Banco: `retrocore_mod2`
   * Usuário: `postgres`
   * Senha: `123456`
   * Porta: `5440` (ou ajustar `application.yml`).

2. Na raiz do projeto:

```bash
./mvnw clean package
```

3. Rodar o serviço de login:

```bash
cd login-spring
java -jar target/login-spring-0.0.1-SNAPSHOT.jar
```

4. Em outro terminal, rodar o gateway:

```bash
cd gateway
../mvnw spring-boot:run
```

---

## 6. Fluxo de autenticação e uso do JWT

1. **Registro**

   * Cliente chama `POST /api/v1/auth/register` com username, password e fullName.
   * Serviço cria o usuário com senha hash (`BCrypt`) e role padrão `"USER"`.
   * Retorna token JWT válido.

2. **Login**

   * Cliente chama `POST /api/v1/auth/login` com username e password.
   * Serviço valida credenciais via `AuthenticateUserUseCase`.
   * Gera novo token JWT.

3. **Uso do token**

   * O token retornado pode ser enviado em outros serviços como:

     ```http
     Authorization: Bearer <token>
     ```
   * Este módulo foca na emissão do token; a validação pode ser feita pelos módulos seguintes da arquitetura.

