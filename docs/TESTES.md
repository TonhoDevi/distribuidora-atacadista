# Testes automatizados: fundamentos e aplicação neste projeto

Guia de estudo. Todos os exemplos são de código real do repositório.

## Índice

1. [Por que testar](#1-por-que-testar)
2. [Tipos de teste e a pirâmide](#2-tipos-de-teste-e-a-pirâmide)
3. [O padrão AAA (Arrange, Act, Assert)](#3-o-padrão-aaa-arrange-act-assert)
4. [Dublês de teste: mock, stub, spy, fake](#4-dublês-de-teste-mock-stub-spy-fake)
5. [Ferramentas: JUnit 5 e Mockito](#5-ferramentas-junit-5-e-mockito)
6. [Anatomia de um teste do projeto, linha a linha](#6-anatomia-de-um-teste-do-projeto-linha-a-linha)
7. [O que testar (e o que não testar)](#7-o-que-testar-e-o-que-não-testar)
8. [Testando exceções e caminhos de falha](#8-testando-exceções-e-caminhos-de-falha)
9. [Testando segurança: o exemplo do JwtService](#9-testando-segurança-o-exemplo-do-jwtservice)
10. [Testes de integração e Testcontainers](#10-testes-de-integração-e-testcontainers)
11. [Mapa dos testes do projeto](#11-mapa-dos-testes-do-projeto)
12. [Como rodar](#12-como-rodar)
13. [Erros comuns](#13-erros-comuns)
14. [O que ainda não está coberto](#14-o-que-ainda-não-está-coberto)
15. [Exercícios](#15-exercícios)

---

## 1. Por que testar

Um teste automatizado é código que **executa o seu código e confere o resultado**. Ele serve para:

- **Pegar bugs cedo**: quebrou uma regra? O teste falha em segundos, antes de chegar ao usuário.
- **Permitir mudar sem medo (refatorar)**: se os testes continuam verdes, o comportamento foi preservado.
- **Documentar o comportamento**: `deveriaLancarExcecaoQuandoEstoqueInsuficiente` diz o que o sistema faz sem precisar ler a implementação.
- **Guiar o design**: código difícil de testar costuma estar muito acoplado.

Exemplo real: ao criar a baixa de estoque no `order-service`, os testes garantiram que, se o salvamento do pedido falha, o estoque volta (compensação). Sem teste, esse caminho raro só seria descoberto em produção.

---

## 2. Tipos de teste e a pirâmide

```
            ▲   poucos, lentos, caros
           ╱ ╲  E2E / ponta a ponta  (navegador → gateway → serviços → banco)
          ╱───╲
         ╱     ╲  Integração           (serviço + banco real, Testcontainers)
        ╱───────╲
       ╱         ╲  Unitários           (uma classe isolada, dependências simuladas)
      ╱───────────╲ muitos, rápidos, baratos
```

| Tipo | O que testa | Dependências | Velocidade | Neste projeto |
|---|---|---|---|---|
| **Unitário** | Uma classe/método isolado (regra de negócio) | Todas simuladas (mocks) | milissegundos | `*ServiceTest` |
| **Integração** | Várias peças juntas (ex.: repositório + Postgres) | Reais | segundos | `CustomerRepositoryIntegrationTest`, `contextLoads` |
| **E2E** | O sistema inteiro, como o usuário | Todas reais | lento | não há |

A pirâmide diz: **muitos unitários, alguns de integração, poucos E2E**. Unitários são rápidos e apontam exatamente onde quebrou; E2E dão confiança geral, mas são lentos e frágeis.

---

## 3. O padrão AAA (Arrange, Act, Assert)

Todo teste bem escrito tem três blocos, sempre na mesma ordem:

| Fase | Pergunta | O que fazer |
|---|---|---|
| **Arrange** (preparar) | Qual é o cenário? | Criar objetos, configurar os mocks (`when(...)`) |
| **Act** (agir) | O que está sendo testado? | Chamar **uma** coisa: o método sob teste |
| **Assert** (verificar) | O que deveria ter acontecido? | `assertEquals`, `verify`, `assertThrows` |

Exemplo do projeto (`CustomerServiceTest`):

```java
@Test
void deveriaCriarClienteComSucesso() {
    // Arrange
    var dto = new CustomerRequestDto("João", "joao@email.com", "12345678900");
    when(customerRepository.existsByDocument(dto.document())).thenReturn(false);
    when(customerRepository.existsByEmail(dto.email())).thenReturn(false);
    when(customerRepository.save(any(Customer.class))).thenReturn(
            new Customer("João", "joao@email.com", "12345678900"));

    // Act
    var resultado = customerService.createCustomer(dto);

    // Assert
    assertEquals("João", resultado.name());
    verify(customerRepository).save(any(Customer.class));
}
```

**Regras de ouro do AAA**
- **Um Act por teste.** Se há dois "agires", provavelmente são dois testes.
- **Quando o Act é uma exceção**, ele se funde com o Assert: `// Act & Assert` com `assertThrows(() -> ...)`.
- **Sem lógica no teste** (sem `if`, `for` complexo). Teste deve ser tão simples que dispensa outro teste.
- **Linhas em branco** separam as fases, e os comentários `// Arrange / Act / Assert` tornam a estrutura visível enquanto você aprende.

> Há um caso em que o teste tem **dois Acts de propósito**: `deveriaUsarMesmaMensagemParaUsuarioInexistenteESenhaErrada` (em `AuthServiceTest`) executa dois logins para **comparar** as mensagens de erro. É a exceção que confirma a regra: aqui a comparação entre as duas execuções é o próprio objeto do teste.

Variações do mesmo padrão: **Given / When / Then** (BDD). É a mesma ideia com outros nomes: Given = Arrange, When = Act, Then = Assert.

---

## 4. Dublês de teste: mock, stub, spy, fake

Um serviço normalmente depende de outras coisas (banco, outro serviço, fila). No teste **unitário** você as substitui por *dublês*, para testar só a sua classe, de forma rápida e previsível.

| Dublê | O que é | Exemplo neste projeto |
|---|---|---|
| **Stub** | Devolve respostas prontas | `when(customerRepository.findById(1L)).thenReturn(Optional.of(customer))` |
| **Mock** | Registra as chamadas para você **verificar** depois | `verify(productClient).decreaseStock(10L, ...)` |
| **Spy** | Objeto real com algumas partes espionadas/trocadas | não usado aqui |
| **Fake** | Implementação simples e funcional (ex.: banco em memória) | não usado aqui |

No Mockito, o mesmo objeto criado com `@Mock` serve como stub (`when`) e como mock (`verify`).

**Stub vs. verify: quando usar cada um**
- Use **`when(...).thenReturn(...)`** para *alimentar* o cenário (o que as dependências respondem).
- Use **`assert...`** para conferir o **resultado** (retorno, exceção): isso é o principal.
- Use **`verify(...)`** para conferir um **efeito colateral importante** que não aparece no retorno: "salvou?", "publicou evento?", "devolveu o estoque?". E `verify(..., never())` para garantir que algo **não** aconteceu (ex.: não salvar quando deu erro).

---

## 5. Ferramentas: JUnit 5 e Mockito

**JUnit 5**: o framework que roda os testes.

| Elemento | Para que serve |
|---|---|
| `@Test` | Marca um método como teste |
| `Assertions.assertEquals(esperado, atual)` | Compara valores (o esperado vem **primeiro**) |
| `assertTrue / assertFalse / assertNotNull` | Verificações booleanas e de nulidade |
| `assertThrows(Excecao.class, () -> ...)` | Garante que o código lança aquela exceção |
| `@Tag("integration")` | Rotula o teste para poder incluir/excluir grupos |

**Mockito**: cria e controla os dublês.

| Elemento | Para que serve |
|---|---|
| `@ExtendWith(MockitoExtension.class)` | Liga o Mockito ao JUnit |
| `@Mock` | Cria o dublê de uma dependência |
| `@InjectMocks` | Cria a classe real sob teste e injeta os `@Mock` nela |
| `when(x.metodo()).thenReturn(v)` | Define a resposta de um dublê |
| `when(...).thenThrow(ex)` / `thenAnswer(...)` | Faz o dublê lançar exceção / calcular a resposta |
| `verify(x).metodo(args)` | Confere que o método foi chamado |
| `verify(x, never()).metodo(...)` / `times(n)` | Confere que **não** foi chamado / foi chamado n vezes |
| `any()`, `eq(v)`, `argThat(...)` | *Matchers*: casam argumentos sem precisar do valor exato |

**Strict stubs**: com `MockitoExtension`, um `when(...)` que **não é usado** pelo teste falha com `UnnecessaryStubbingException`. Isso é útil: avisa que o Arrange tem coisa sobrando.

---

## 6. Anatomia de um teste do projeto, linha a linha

`OrderServiceTest.deveriaDevolverEstoqueQuandoFalhaAoSalvarPedido`: o teste mais rico do projeto.

```java
@Test
void deveriaDevolverEstoqueQuandoFalhaAoSalvarPedido() {
    // Arrange
    when(resilientClient.getCustomer(1L)).thenReturn(clienteExistente());      // (1)
    when(resilientClient.getProduct(10L)).thenReturn(produtoComEstoque(100));  // (2)
    when(orderRepository.save(any(Order.class)))
            .thenThrow(new RuntimeException("db down"));                       // (3)

    // Act
    assertThrows(RuntimeException.class,
            () -> orderService.createOrder(pedidoDe(2)));                      // (4)

    // Assert: compensação — o estoque baixado volta
    verify(productClient).increaseStock(10L, new StockAdjustmentDto(2));       // (5)
    verify(rabbitTemplate, never())
            .convertAndSend(anyString(), anyString(), any(Object.class));      // (6)
}
```

1. O cliente existe (o `order-service` consulta o `customer-service`; aqui é um dublê).
2. O produto existe e tem 100 em estoque.
3. **O cenário de falha**: o banco "cai" na hora de salvar.
4. O pedido é criado e a exceção sobe. (Aqui o Act é embrulhado no `assertThrows` apenas para o teste não explodir; a verificação de verdade vem depois.)
5. **O que importa**: o estoque que já tinha sido baixado foi devolvido.
6. E nenhum evento "pedido criado" foi publicado, pois o pedido não existe.

Repare nos **helpers** (`clienteExistente()`, `produtoComEstoque(...)`, `pedidoDe(...)`): eles removem repetição do Arrange e dão nome ao cenário, o que deixa o teste legível.

**Nomenclatura**: `deveria<Comportamento>Quando<Condição>`. O nome é a especificação. Quando o teste falhar, o nome já diz o que quebrou.

---

## 7. O que testar (e o que não testar)

**Teste o comportamento, não a implementação.** Pergunte: "se eu reescrever o método por dentro mantendo o resultado, o teste continua passando?" Se não, ele está acoplado demais.

**Priorize**
- Regras de negócio (cálculo de total, baixa de estoque, transição de status).
- **Caminhos de erro** (cliente inexistente, SKU duplicado, estoque insuficiente), onde moram os bugs.
- **Valores de borda**: estoque exatamente igual ao pedido, quantidade zero, lista vazia.
- Segurança (token expirado, assinatura inválida, senha errada).

**Em geral não vale testar**
- Getters/setters triviais.
- Código do framework (que o Spring serializa JSON, que o JPA salva).
- Configuração pura.

**Cobertura (%) não é qualidade.** 100% de cobertura com asserts fracos é pior que 70% bem pensados. Use a cobertura para achar o que **não** foi testado, não como meta.

---

## 8. Testando exceções e caminhos de falha

O **caminho feliz** (tudo dá certo) é o mais fácil; os bugs ficam nos outros. Para cada regra, pergunte "como isso falha?":

| Regra | Teste de falha correspondente |
|---|---|
| SKU é único | `deveriaLancarExcecaoQuandoSkuJaExiste` |
| Estoque não pode ficar negativo | `deveriaLancarExcecaoQuandoEstoqueInsuficiente` |
| Pedido entregue não pode ser cancelado | `deveriaRejeitarTransicaoDeStatusInvalida` |
| Cliente tem que existir | `deveriaLancarExcecaoQuandoClienteNaoEncontrado` |
| Preço não pode ser negativo | `deveriaLancarExcecaoDeDadosInvalidosAoAtualizarComPrecoNegativo` |

Um teste de falha deve conferir **duas coisas**: (a) a exceção certa foi lançada e (b) **nada de ruim aconteceu depois**:

```java
// Act & Assert
assertThrows(CustomerAlreadyExistsException.class, () -> customerService.createCustomer(dto));
verify(customerRepository, never()).save(any());   // não salvou nada
```

Sem o `never()`, o teste passaria mesmo se o código salvasse o cliente duplicado e **depois** lançasse a exceção.

> **Um bug que esse tipo de teste ajuda a encontrar.** Ao revisar para este documento, achei que `updateProduct` lançava `IllegalArgumentException` para preço negativo, enquanto `createProduct` lançava `InvalidProductDataException`. Como só a segunda tem handler, atualizar com preço negativo devolvia **500** em vez de **400**. Corrigi e adicionei o teste que prende o comportamento. Veja em [HTTP-STATUS.md](HTTP-STATUS.md) por que isso importa.

---

## 9. Testando segurança: o exemplo do JwtService

`JwtServiceTest` mostra como testar **propriedades de segurança**, não só o caminho feliz:

| Teste | Garante |
|---|---|
| `deveriaGerarTokenComSubjectERoleCorretos` | O token carrega usuário e papel |
| `deveriaRejeitarTokenExpirado` | Token vencido não vale (validade negativa cria um token já expirado) |
| `deveriaRejeitarTokenAssinadoComOutraChave` | Quem não tem a chave não consegue forjar |
| `deveriaRejeitarTokenAdulterado` | Trocar `GERENTE` por `ADMIN` no payload invalida a assinatura |
| `deveriaRejeitarTextoQueNaoEUmJwt` | Lixo não vira credencial |

Veja `deveriaRejeitarTokenAdulterado`: ele **ataca** o próprio sistema (fabrica um payload forjado reaproveitando a assinatura antiga) e confirma que a defesa funciona. Esse estilo, "tente quebrar e prove que não quebra", é a forma mais valiosa de teste de segurança. O funcionamento do JWT está explicado em [JWT.md](JWT.md).

Outro exemplo: `AuthServiceTest.deveriaUsarMesmaMensagemParaUsuarioInexistenteESenhaErrada` prende a proteção contra *user enumeration* (não revelar se o usuário existe).

---

## 10. Testes de integração e Testcontainers

Testes unitários com mocks **não provam** que o SQL, o mapeamento JPA ou a configuração do Spring funcionam. Para isso existem os de integração: sobem a aplicação (ou parte dela) com dependências **reais**.

**Testcontainers** sobe um container Docker descartável só para o teste:

```java
@Tag("integration")
@SpringBootTest
@Testcontainers
class CustomerRepositoryIntegrationTest {

    @Container
    @ServiceConnection                       // o Spring conecta sozinho neste Postgres
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void deveriaSalvarClienteNoBancoReal() {
        // Arrange
        Customer customer = new Customer("Maria", "maria@email.com", "99988877766");

        // Act
        Customer saved = customerRepository.save(customer);

        // Assert
        assertNotNull(saved.getId());          // o banco gerou o ID
        assertEquals("Maria", saved.getName());
    }
}
```

Vantagem: banco **limpo e isolado** a cada execução, igual ao de produção (Postgres 16), sem depender de nada instalado na máquina. Desvantagem: precisa de Docker e é bem mais lento.

**`contextLoads`**: o teste que o Spring Initializr gera em cada serviço. Só confirma que a aplicação **sobe** (contexto Spring monta, Flyway roda, conexões abrem). Ele precisa de Postgres/RabbitMQ/Eureka reais, então falha num `mvn test` comum.

### Como o projeto separa os dois mundos

Os testes que precisam de infraestrutura estão marcados com `@Tag("integration")`, e o `pom.xml` de cada serviço os **exclui por padrão**:

```xml
<excludedGroups>integration</excludedGroups>
```

Resultado: `mvn test` é rápido, não depende de Docker e fica verde em qualquer máquina. Para rodar os de integração, veja a próxima seção.

---

## 11. Mapa dos testes do projeto

**Total no `mvn test` padrão: 55 testes, todos passando** (unitários + `contextLoads` dos serviços que não precisam de infra).

| Serviço | Arquivo | Qtd. | O que cobre |
|---|---|---|---|
| auth-service | `AuthServiceTest` | 4 | Login ok, senha errada, usuário inexistente, mensagem genérica (anti-enumeração) |
| | `JwtServiceTest` | 5 | Geração, expiração, chave errada, adulteração, token inválido |
| | `UserServiceTest` | 4 | Criar com senha criptografada, atualizar, deletar, deletar inexistente |
| | `AuthCookieServiceTest` | 3 | Flags do cookie (HttpOnly, Secure, SameSite, Max-Age) e limpeza |
| customer-service | `CustomerServiceTest` | 10 | CRUD, documento/e-mail duplicados, inexistentes |
| product-service | `ProductServiceTest` | 13 | CRUD, SKU duplicado, baixa e devolução de estoque atômicas, dados inválidos |
| order-service | `OrderServiceTest` | 13 | Criar pedido, total calculado no servidor, estoque, compensação, evento, status |
| eureka / gateway / notification | `*ApplicationTests` | 1 cada | A aplicação sobe |

**Marcados como `integration` (fora do `mvn test` padrão)**

| Serviço | Teste | Precisa de |
|---|---|---|
| customer-service | `CustomerRepositoryIntegrationTest` | Docker (Testcontainers) |
| auth / customer / product / order | `*ApplicationTests.contextLoads` | Postgres (e RabbitMQ no order) no ar |

**Frontend**: `app.spec.ts` (Vitest) com um teste básico de criação do componente raiz.

---

## 12. Como rodar

Dentro da pasta de um serviço:

```bash
./mvnw test                                   # unitários (rápido, sem Docker)
./mvnw test -Dtest=OrderServiceTest           # só uma classe
./mvnw test -Dtest=OrderServiceTest#deveriaCancelarPedidoEDevolverEstoque   # só um método
```

Incluir os de integração (precisa de Docker/infra no ar):

```bash
./mvnw test -DexcludedGroups=nenhum                                          # tudo
./mvnw test -DexcludedGroups=nenhum -Dtest=CustomerRepositoryIntegrationTest # só o Testcontainers
```

Frontend (precisa de `npm install` antes):

```bash
cd frontend && npm test
```

> Dica para PCs fracos: rode **um serviço por vez**. Os unitários são leves; os de integração (Spring + Docker) é que pesam.

---

## 13. Erros comuns

| Erro | Por que é ruim | Como evitar |
|---|---|---|
| **Mockar demais** (mockar até o que você testa) | O teste passa mesmo com o código quebrado | Mocke só as **fronteiras** (banco, rede, fila) |
| **`verify` em tudo** | Acopla o teste à implementação; refatorar quebra o teste | Verifique só efeitos relevantes |
| **Vários Acts / vários motivos de falha** | Quando falha, não se sabe por quê | Um comportamento por teste |
| **Testes dependentes entre si / de ordem** | Falhas misteriosas e intermitentes | Cada teste monta o seu próprio cenário |
| **Teste que sempre passa** (assert fraco, sem assert) | Falsa sensação de segurança | Teste "no vermelho" primeiro: quebre o código de propósito e veja o teste falhar |
| **Dados mágicos** (`"abc"`, `42` sem contexto) | Ninguém entende o cenário | Helpers com nomes (`produtoComEstoque(100)`) |
| **Testar só o caminho feliz** | Os bugs estão nos erros e nas bordas | Para cada regra, escreva a falha |
| **Teste lento ou instável** | Ninguém roda, e o time perde a confiança | Unitário sem rede/disco/relógio; integração isolada e marcada |

**Princípios FIRST**: testes **F**ast (rápidos), **I**ndependent (independentes), **R**epeatable (repetíveis em qualquer máquina), **S**elf-validating (passam ou falham sozinhos, sem olho humano), **T**imely (escritos junto com o código).

---

## 14. O que ainda não está coberto

Seja honesto sobre as lacunas: saber o que **não** está testado é parte de testar bem.

- **Controllers** (`@WebMvcTest` com `MockMvc`): confirmariam os códigos HTTP e o JSON de cada endpoint.
- **Handlers de exceção**: o mapeamento exceção → status HTTP não tem teste (veja [HTTP-STATUS.md](HTTP-STATUS.md)).
- **`JwtGlobalFilter` do Gateway**: é a peça de segurança mais importante e não tem teste (cookie ausente → 401, role insuficiente → 403).
- **`ResilientExternalServiceClient`**: circuit breaker e timeout.
- **`PedidoCriadoConsumer`** e a publicação real no RabbitMQ.
- **Frontend**: guards, interceptors e serviços não têm testes; só existe o `app.spec.ts`.
- **E2E**: nenhum teste exercita o fluxo completo (login → criar pedido → baixar estoque).

---

## 15. Exercícios

1. **Quebre de propósito.** No `OrderService`, comente a linha que chama `restoreStock(reserved)` no `catch`. Rode os testes: qual falha? Isso prova que o teste realmente protege a regra. Desfaça depois.
2. **Valor de borda.** Escreva um teste: pedido de exatamente a quantidade em estoque (`pedidoDe(100)` com estoque 100) deve ser aceito. E com 101, rejeitado.
3. **Novo cenário.** Em `CustomerService`, adicione teste para `findCustomerByEmail` inexistente.
4. **Controller.** Crie um `@WebMvcTest(CustomerController.class)` que valide que `POST /customers` com e-mail inválido retorna **400**.
5. **Teste do Gateway.** Escreva um teste do `JwtGlobalFilter` (use `MockServerWebExchange`) para: sem cookie → 401; token válido de `GERENTE` em `DELETE` → 403.
6. **Mutação mental.** Pegue qualquer teste e pergunte: "se eu trocar `>=` por `>` no código, algum teste falha?" Se não, falta um teste de borda.
