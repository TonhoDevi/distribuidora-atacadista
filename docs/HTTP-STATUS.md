# Códigos de status HTTP: o que significam e como este projeto os usa

Guia de estudo sobre 200, 201, 400, 401, 403, 404, 409, 500 e companhia, sempre ligado ao código real do projeto.

## Índice

1. [A ideia central](#1-a-ideia-central)
2. [As 5 famílias](#2-as-5-famílias)
3. [Os códigos que você precisa dominar](#3-os-códigos-que-você-precisa-dominar)
4. [Os confusos: 400 vs 422 vs 409, e 401 vs 403 vs 404](#4-os-confusos)
5. [Como o projeto devolve cada código](#5-como-o-projeto-devolve-cada-código)
6. [O formato do erro: StandardError](#6-o-formato-do-erro-standarderror)
7. [Como o Spring transforma exceção em status](#7-como-o-spring-transforma-exceção-em-status)
8. [Como o frontend reage a cada código](#8-como-o-frontend-reage-a-cada-código)
9. [Verbos HTTP e o status certo para cada um](#9-verbos-http-e-o-status-certo-para-cada-um)
10. [Problemas conhecidos neste projeto (para estudar)](#10-problemas-conhecidos-neste-projeto-para-estudar)
11. [Pratique com curl](#11-pratique-com-curl)
12. [Perguntas de entrevista](#12-perguntas-de-entrevista)
13. [Exercícios](#13-exercícios)

---

## 1. A ideia central

Toda resposta HTTP começa com uma linha de status:

```
HTTP/1.1 404 Not Found
         └┬┘ └───┬───┘
        código  frase (só para humanos; o que vale é o número)
```

O **código** é o contrato entre servidor e cliente: ele diz, de forma padronizada, **o que aconteceu com o pedido**. O cliente (navegador, app, outro serviço) decide o que fazer **só pelo número**, sem ler a mensagem:

- `200`: use os dados.
- `401`: peça login.
- `404`: mostre "não encontrado".
- `503`: tente de novo mais tarde.

Por isso **o código importa mais que o texto**. Devolver `200` com `{"erro": "não encontrado"}` no corpo é um erro clássico: quebra caches, ferramentas de monitoramento e o tratamento de erro de todo cliente.

**Quem é o culpado?** A pergunta que organiza tudo:

| Faixa | De quem é o problema |
|---|---|
| `2xx` | Ninguém: deu certo |
| `4xx` | **Do cliente** (pediu errado). Repetir o mesmo pedido dará o mesmo erro |
| `5xx` | **Do servidor** (falhou ao atender um pedido válido). Repetir pode funcionar depois |

---

## 2. As 5 famílias

| Família | Significado | Exemplos |
|---|---|---|
| **1xx** Informativo | "Recebi, continue" | `100 Continue` (raro no dia a dia) |
| **2xx** Sucesso | Deu certo | `200`, `201`, `204` |
| **3xx** Redirecionamento | Procure em outro lugar | `301`, `302`, `304 Not Modified` |
| **4xx** Erro do cliente | O pedido tem problema | `400`, `401`, `403`, `404`, `409` |
| **5xx** Erro do servidor | O servidor falhou | `500`, `502`, `503`, `504` |

---

## 3. Os códigos que você precisa dominar

### Sucesso (2xx)

| Código | Nome | Quando usar | No projeto |
|---|---|---|---|
| **200** | OK | Deu certo e há corpo na resposta | `GET /customers`, `PUT /products/1`, `POST /auth/login` |
| **201** | Created | **Criou** um recurso novo | `POST /customers`, `POST /orders`, `POST /users` |
| **204** | No Content | Deu certo e **não há corpo** | `DELETE /customers/1`, `POST /auth/logout` |

`201` é o certo para `POST` que cria. `204` é típico de `DELETE` (não há o que devolver depois de apagar).

### Erro do cliente (4xx)

| Código | Nome | Significado | No projeto |
|---|---|---|---|
| **400** | Bad Request | Pedido malformado ou dados inválidos | Campo vazio, e-mail inválido, preço negativo |
| **401** | Unauthorized | **Não autenticado**: sem credencial, ou credencial inválida/expirada | Sem cookie/token, token vencido, senha errada no login |
| **403** | Forbidden | **Autenticado, mas sem permissão** | `GERENTE` tentando `DELETE` |
| **404** | Not Found | O recurso não existe | `GET /customers/999`, cliente inexistente num pedido |
| **405** | Method Not Allowed | O caminho existe, mas não aceita esse verbo | `PUT /orders/1` (só há `PATCH /status`) |
| **409** | Conflict | O pedido é válido, mas **conflita com o estado atual** | SKU/e-mail/documento duplicado, estoque insuficiente, transição de status inválida |
| **422** | Unprocessable Content | Sintaxe ok, mas regra semântica violada | não usado aqui (veja a seção 4) |
| **429** | Too Many Requests | Limite de requisições estourado (rate limit) | não implementado |

### Erro do servidor (5xx)

| Código | Nome | Significado | No projeto |
|---|---|---|---|
| **500** | Internal Server Error | Bug ou falha inesperada no servidor | Exceção não tratada (NullPointer, falha de SQL) |
| **502** | Bad Gateway | O gateway recebeu resposta inválida do serviço de trás | Gateway com serviço quebrado |
| **503** | Service Unavailable | Servidor temporariamente indisponível ou sobrecarregado | Circuit breaker aberto, serviço fora do ar |
| **504** | Gateway Timeout | O gateway esperou demais pelo serviço de trás | Serviço lento demais |

---

## 4. Os confusos

### 401 vs 403: "quem é você?" vs "você pode?"

```
Requisição chega
      │
      ├─ Sem credencial / token inválido ou expirado ──► 401  (autenticação falhou)
      │
      └─ Credencial ok, mas o papel não permite ───────► 403  (autorização falhou)
```

| | 401 | 403 |
|---|---|---|
| Pergunta | Quem é você? | Você tem permissão? |
| Resolve com | Fazer login de novo | Nada (a conta não tem o direito) |
| Exemplo | Cookie ausente ou expirado | `GERENTE` chamando `DELETE /customers/1` |

> Apesar do nome "Unauthorized", o **401 é sobre autenticação**. O nome é um erro histórico do HTTP.

### 400 vs 422 vs 409

| Código | Use quando | Exemplo |
|---|---|---|
| **400** | O pedido está **mal formado ou com dado inválido** | `{"email": "abc"}` (não é e-mail), JSON quebrado, campo obrigatório vazio |
| **422** | A estrutura está certa, mas **não faz sentido** para a regra de negócio | (alguns times usam; este projeto usa 400 para validação) |
| **409** | O pedido é válido, mas **bate no estado atual** do sistema | Cadastrar e-mail que já existe; cancelar pedido já entregue; baixar mais estoque do que existe |

Regra prática: **se o mesmo pedido poderia dar certo em outro momento** (porque o estado mudaria), é **409**. Se **nunca** vai dar certo do jeito que foi enviado, é **400**.

### 404 vs 403 (esconder a existência)

Às vezes se devolve `404` em vez de `403` para **não revelar que o recurso existe** (ex.: o pedido de outro cliente). É uma escolha de segurança; neste projeto não é necessário.

### 500 vs 4xx: nunca devolva 500 por erro do cliente

Se o usuário mandou um dado inválido e o servidor responde `500`, está **mentindo sobre quem errou**. O cliente acha que o sistema quebrou, o alarme de monitoramento dispara à toa, e o usuário não descobre o que corrigir. **Todo erro previsível deve virar um 4xx claro.** O `500` fica só para o que você realmente não previu.

---

## 5. Como o projeto devolve cada código

Quem gera cada resposta:

| Código | Quem devolve | Onde no código |
|---|---|---|
| **200 / 201 / 204** | O controller | `ResponseEntity.ok(...)`, `new ResponseEntity<>(..., HttpStatus.CREATED)`, `ResponseEntity.noContent().build()` |
| **400** (validação) | Handler de `MethodArgumentNotValidException` | Dispara quando um `@Valid` falha (`@NotBlank`, `@Email`, `@Positive`...) |
| **400** (regra) | `InvalidProductDataException` | `ProductExceptionHandler` |
| **401** (login) | `InvalidCredentialsException` | `UserExceptionHandler` no auth-service |
| **401** (token) | `JwtGlobalFilter` | Cookie/header ausente, assinatura inválida ou token expirado |
| **403** | `JwtGlobalFilter.isAuthorized` | `DELETE` e `POST /users` só para `ADMIN` |
| **404** | `*NotFoundException` | `CustomerNotFound`, `ProductNotFound`, `OrderNotFound`, `UserNotFound` |
| **409** | `*AlreadyExists`, `InsufficientStock`, `InvalidStatusTransition` | Handlers de cada serviço |
| **500** | Spring (padrão) | Qualquer exceção **sem** handler |

### Mapa por endpoint (via Gateway)

| Requisição | Possíveis respostas |
|---|---|
| `POST /auth/login` | 200 (ok), 400 (campo vazio), 401 (usuário/senha inválidos) |
| `POST /customers` | 201, 400 (campos inválidos), 401, 409 (documento/e-mail duplicado) |
| `GET /customers/{id}` | 200, 401, 404 |
| `DELETE /customers/{id}` | 204, 401, 403 (não é ADMIN), 404 |
| `POST /orders` | 201, 400, 401, 404 (cliente/produto inexistente), 409 (estoque insuficiente) |
| `PATCH /orders/{id}/status` | 200, 401, 404, 409 (transição inválida) |
| `POST /products/{id}/stock/decrease` | 200, 404, 409 (sem estoque) |

### A ordem em que as barreiras acontecem

```
Navegador ──► Gateway ──────────────────► Serviço ───────────► Banco
              │                            │
              ├─ sem token ........ 401    ├─ @Valid falhou ... 400
              ├─ token inválido ... 401    ├─ não achou ....... 404
              └─ sem permissão .... 403    ├─ conflito ........ 409
                                           └─ bug ............. 500
```

O Gateway barra **antes** de chegar ao serviço; por isso uma requisição sem token nunca chega a ser validada (400) nem procurada (404).

---

## 6. O formato do erro: StandardError

Todos os serviços respondem erros no mesmo formato (record `StandardError`):

```json
{
  "timestamp": "05/10/2026 - 14:30:00",
  "status": 409,
  "message": "Customer already exists with email: joao@email.com",
  "path": "/customers"
}
```

Por que padronizar? O frontend lê **sempre** `body.message` e mostra ao usuário (veja `NotificationService.error`). Cada serviço com um formato diferente obrigaria o cliente a tratar caso a caso.

Boas práticas de mensagem de erro:
- Diga **o que** deu errado e, se possível, **como corrigir**.
- **Não vaze detalhes internos** (stack trace, SQL, nomes de tabela): isso ajuda atacantes.
- No login, use mensagem **genérica** ("Invalid username or password") para não revelar se o usuário existe.

---

## 7. Como o Spring transforma exceção em status

O padrão usado em todos os serviços: **o service lança uma exceção de domínio; um `@RestControllerAdvice` a converte em resposta HTTP.**

```java
// 1) O service expressa a regra de negócio lançando uma exceção com nome claro
if (productRepository.existsBySku(dto.sku())) {
    throw new ProductAlreadyExistsException("Product already exists with sku: " + dto.sku());
}

// 2) O handler traduz a exceção em status + corpo padronizado
@RestControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<StandardError> handleProductAlreadyExists(
            ProductAlreadyExistsException ex, HttpServletRequest request) {

        StandardError error = new StandardError(
                LocalDateTime.now(), HttpStatus.CONFLICT.value(),   // 409
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
```

**Por que esse desenho é bom**
- O `service` **não conhece HTTP**: só diz "SKU já existe". Isso o torna testável (veja [TESTES.md](TESTES.md)) e reaproveitável.
- A regra "SKU duplicado = 409" fica **num lugar só** (o handler).
- Exceções com nome (`InsufficientStockException`) documentam o domínio.

**Sem handler, o que acontece?** O Spring devolve **500** com o corpo padrão dele (não o `StandardError`). É por isso que **toda exceção de erro do cliente precisa de handler**.

---

## 8. Como o frontend reage a cada código

| Código | Reação do frontend | Onde |
|---|---|---|
| **401** (fora do login) | Limpa a sessão e redireciona para `/login` | `error.interceptor.ts` |
| **401 no login** | Mostra "Usuário ou senha inválidos." | `login.component.ts` |
| **400 / 404 / 409** | Mostra a `message` do `StandardError` num snackbar | `NotificationService.error` |
| **Sem resposta** (rede/servidor caído) | Mostra a mensagem genérica de fallback | `NotificationService.error` |
| **403** | Tratado só como erro genérico; o `roleGuard` já esconde as telas proibidas (apenas UX) | `role.guard.ts` |

A regra: **o frontend decide a ação pelo código (401 → login) e a mensagem pelo corpo (`message`)**.

---

## 9. Verbos HTTP e o status certo para cada um

| Verbo | Faz | Sucesso | Idempotente? | Neste projeto |
|---|---|---|---|---|
| **GET** | Lê | 200 | Sim (não altera nada) | listagens e buscas |
| **POST** | Cria / executa ação | 201 (criou) ou 200 | **Não** (repetir cria outro) | criar cliente, pedido; login |
| **PUT** | Substitui o recurso inteiro | 200 | Sim | `PUT /customers/1` |
| **PATCH** | Altera parte do recurso | 200 | Depende | `PATCH /orders/1/status` |
| **DELETE** | Remove | 204 | Sim | `DELETE /products/1` |

**Idempotente** = repetir a mesma requisição N vezes tem o **mesmo efeito** de fazer uma vez. `DELETE /products/1` duas vezes: o produto some na primeira; a segunda costuma dar `404`, mas o **estado final** é o mesmo (produto não existe). Já `POST /orders` duas vezes cria **dois pedidos**.

Detalhe: `POST /products/{id}/stock/decrease` **não** é idempotente (cada chamada baixa mais estoque), e é por isso que o `order-service` precisa tomar cuidado com tentativas repetidas.

---

## 10. Problemas conhecidos neste projeto (para estudar)

Estes pontos foram encontrados na revisão; são bons casos para entender o assunto na prática.

1. **Não existe handler genérico (`Exception.class`).** Qualquer erro inesperado devolve o 500 padrão do Spring, em formato diferente do `StandardError`. O correto é ter um handler final que devolva `500` no mesmo formato e **sem vazar detalhes**, registrando o erro em log.

2. **Serviço fora do ar vira 404.** No `order-service`, quando o `customer-service` está indisponível (timeout ou circuito aberto), o fallback lança `CustomerNotFoundException`, e o cliente da API recebe **404 "Customer not found"**. Semanticamente deveria ser **503 Service Unavailable**: o cliente existe, só não dá para consultá-lo agora. É um bom exercício: criar uma `ServiceUnavailableException` e mapeá-la para 503.

3. **`auth-service` acessado direto responde 403 (e não 401) sem token.** É o comportamento padrão do Spring Security quando não há `AuthenticationEntryPoint` configurado. Pelo Gateway o cliente recebe 401 corretamente, mas direto na porta do serviço sai 403.

4. **Já corrigido:** `updateProduct` lançava `IllegalArgumentException` para preço/estoque negativo, sem handler, o que causava **500** onde deveria ser **400**. Agora usa `InvalidProductDataException`, igual ao `createProduct`.

---

## 11. Pratique com curl

Com o sistema no ar (Gateway em `localhost:48080`). A opção `-i` mostra o status; `-c`/`-b` guardam e enviam o cookie.

```bash
# 401: sem credencial
curl -i http://localhost:48080/customers

# 400: login com campos vazios
curl -i -X POST http://localhost:48080/auth/login \
  -H "Content-Type: application/json" -d '{"username":"","password":""}'

# 401: senha errada
curl -i -X POST http://localhost:48080/auth/login \
  -H "Content-Type: application/json" -d '{"username":"TonhoDevi","password":"errada"}'

# 200 + Set-Cookie: login correto
curl -i -c cookies.txt -X POST http://localhost:48080/auth/login \
  -H "Content-Type: application/json" -d '{"username":"TonhoDevi","password":"SUA_SENHA"}'

# 200: com cookie
curl -i -b cookies.txt http://localhost:48080/customers

# 404: recurso inexistente
curl -i -b cookies.txt http://localhost:48080/customers/999999

# 201: criar
curl -i -b cookies.txt -X POST http://localhost:48080/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Loja Teste","email":"teste@loja.com","document":"11122233344"}'

# 409: repetir o mesmo cadastro
curl -i -b cookies.txt -X POST http://localhost:48080/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Loja Teste","email":"teste@loja.com","document":"11122233344"}'

# 400: e-mail inválido
curl -i -b cookies.txt -X POST http://localhost:48080/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"X","email":"nao-e-email","document":"1"}'

# 204: apagar (como ADMIN)
curl -i -b cookies.txt -X DELETE http://localhost:48080/customers/1

# 405: verbo não permitido
curl -i -b cookies.txt -X PUT http://localhost:48080/orders/1
```

Para ver **403**: crie um usuário `GERENTE` (`POST /users` como ADMIN), faça login com ele e tente um `DELETE`.

Para ver **409 de estoque**: crie um pedido com quantidade maior que o estoque do produto.

---

## 12. Perguntas de entrevista

**Qual a diferença entre 401 e 403?**
401: não autenticado (não sei quem você é). 403: autenticado, mas sem permissão.

**Quando usar 409 e quando 400?**
400: dado inválido/mal formado. 409: pedido válido que conflita com o estado atual (duplicidade, regra de negócio dependente do estado).

**Qual status um `POST` que cria deve retornar? E um `DELETE`?**
`201 Created` (de preferência com o recurso criado) e `204 No Content`.

**O que é uma operação idempotente? Cite exemplos.**
Repetir tem o mesmo efeito de executar uma vez: GET, PUT, DELETE. POST normalmente não é.

**Por que não devolver 200 com uma mensagem de erro no corpo?**
Quebra o contrato: clientes, caches, proxies e monitoramento decidem pelo código. Um erro disfarçado de sucesso é invisível para eles.

**Diferença entre 502, 503 e 504?**
502: resposta inválida do serviço de trás. 503: indisponível (fora ou sobrecarregado). 504: o serviço de trás demorou demais.

**Um erro de validação deve ser 400 ou 500? Por quê?**
400: a falha é do cliente. 500 diria que o servidor quebrou, e o monitoramento alertaria à toa.

**Como você padroniza erros numa API?**
Exceções de domínio no service + um `@RestControllerAdvice` convertendo em status e num corpo único (como o `StandardError`).

---

## 13. Exercícios

1. **Handler genérico.** Crie um `@ExceptionHandler(Exception.class)` que devolva `500` no formato `StandardError`, com mensagem genérica, e registre a exceção no log.
2. **Corrigir o 404 enganoso.** Crie `ServiceUnavailableException` (503) e use-a nos fallbacks do `ResilientExternalServiceClient` quando o erro for timeout ou circuito aberto, mantendo 404 só para "não encontrado" de verdade.
3. **401 no auth-service.** Configure um `AuthenticationEntryPoint` no `SecurityConfig` para devolver 401 (e não 403) quando faltar token.
4. **Teste de contrato.** Com `@WebMvcTest` e `MockMvc`, escreva testes que conferem o status (`status().isConflict()`, `isNotFound()`...) para cada exceção do `CustomerController`.
5. **Mapeie sozinho.** Para cada exceção do projeto (`grep -r "extends RuntimeException"`), diga qual status ela deveria produzir e confira se o handler concorda.
6. **Rate limit.** Pesquise como devolver `429` com `Retry-After` e onde isso ficaria no Gateway.
