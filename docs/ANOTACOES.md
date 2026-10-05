# Anotações em Java e Spring: o que são, como funcionam e para que serve cada uma

Guia de estudo. Cobre as anotações usadas neste projeto (`@Override`, `@Service`, `@Entity`, `@Transactional`...) e as principais que você vai encontrar por aí.

## Índice

1. [O que é uma anotação](#1-o-que-é-uma-anotação)
2. [Como o Spring usa anotações (a "mágica" explicada)](#2-como-o-spring-usa-anotações-a-mágica-explicada)
3. [Anotações do Java puro](#3-anotações-do-java-puro)
4. [Aplicação e configuração do Spring Boot](#4-aplicação-e-configuração-do-spring-boot)
5. [Estereótipos: @Component, @Service, @Repository...](#5-estereótipos-component-service-repository)
6. [Injeção de dependência](#6-injeção-de-dependência)
7. [Web e REST](#7-web-e-rest)
8. [Tratamento de erros](#8-tratamento-de-erros)
9. [Validação (Bean Validation)](#9-validação-bean-validation)
10. [Persistência (JPA)](#10-persistência-jpa)
11. [Transações: @Transactional](#11-transações-transactional)
12. [Spring Cloud: Eureka e Feign](#12-spring-cloud-eureka-e-feign)
13. [Resiliência: Resilience4j](#13-resiliência-resilience4j)
14. [Mensageria: RabbitMQ](#14-mensageria-rabbitmq)
15. [Segurança e JSON](#15-segurança-e-json)
16. [Anotações de teste](#16-anotações-de-teste)
17. [Mapa: onde cada anotação aparece no projeto](#17-mapa-onde-cada-anotação-aparece-no-projeto)
18. [Armadilhas comuns](#18-armadilhas-comuns)
19. [Como criar sua própria anotação](#19-como-criar-sua-própria-anotação)
20. [Perguntas de entrevista](#20-perguntas-de-entrevista)
21. [Exercícios](#21-exercícios)

---

## 1. O que é uma anotação

Uma anotação (`@Algo`) é um **metadado**: uma etiqueta colocada sobre classe, método, campo ou parâmetro que **descreve ou instrui** alguém. Ela **não executa nada sozinha**. Quem lê a etiqueta (o compilador, uma biblioteca ou um framework) é quem age.

```java
@Service                       // etiqueta: "esta classe é um serviço"
public class ProductService { ... }
```

Analogia: uma etiqueta "FRÁGIL" numa caixa não protege nada por si só. O que protege é o carregador que **lê a etiqueta** e trata a caixa com cuidado.

### Quem lê as anotações

| Quando é lida | Quem lê | Exemplo |
|---|---|---|
| **Compilação** | O compilador (`javac`) | `@Override` (erro se não sobrescrever nada) |
| **Compilação** (processadores) | Geradores de código | Lombok, MapStruct |
| **Execução** (*runtime*) | Frameworks, via **reflexão** | Spring, JPA, JUnit, Jackson |

A maior parte do que você usa em Spring é lida **em tempo de execução**, quando a aplicação sobe.

### Anatomia

```java
@Column(name = "stock_quantity", nullable = false)
//      └────── parâmetros (atributos) ─────────┘
private int stockQuantity;
```

- Pode ter **parâmetros**: `@Column(name = "x")`. Com um único parâmetro chamado `value`, o nome pode ser omitido: `@RequestMapping("/orders")`.
- Pode ir em: classes, métodos, campos, parâmetros, construtores (depende do que a anotação permite).
- Várias podem ser empilhadas.

---

## 2. Como o Spring usa anotações (a "mágica" explicada)

Quando uma aplicação Spring Boot sobe, acontece, em resumo:

```
1. @SpringBootApplication  →  liga o "component scan" (varre os pacotes)
2. O Spring procura classes com @Component / @Service / @Repository / @RestController...
3. Cria UM objeto de cada (um "bean") e guarda no "contêiner" (ApplicationContext)
4. Para cada construtor, vê os tipos dos parâmetros e entrega os beans certos (injeção)
5. Embrulha alguns beans em "proxies" (@Transactional, @CircuitBreaker...)
6. Registra os endpoints (@GetMapping...) e começa a atender requisições
```

Os conceitos-chave:

| Termo | Significado |
|---|---|
| **Bean** | Objeto criado e gerenciado pelo Spring |
| **Contêiner (ApplicationContext)** | O "armário" que guarda todos os beans |
| **Component scan** | A varredura que encontra classes anotadas |
| **Injeção de dependência (DI)** | O Spring entrega ao construtor os beans de que ele precisa |
| **Proxy** | Objeto que "embrulha" outro para adicionar comportamento (transação, circuit breaker) sem mudar o código |

Você **nunca faz `new ProductService()`**: o Spring cria e entrega. É a chamada **inversão de controle (IoC)**: quem controla a criação dos objetos é o framework, não você.

---

## 3. Anotações do Java puro

### `@Override`
Diz ao compilador: "este método **sobrescreve** um da superclasse ou implementa um de interface". Se você errar o nome ou os parâmetros, **a compilação falha** (sem a anotação, você criaria um método novo sem perceber).

```java
// JwtGlobalFilter implements GlobalFilter, Ordered
@Override
public int getOrder() {        // se eu digitasse getOrdem(), o compilador acusaria erro
    return -1;
}
```

Onde aparece no projeto: `JwtGlobalFilter` (`filter`, `getOrder`), `JwtAuthenticationFilter` (`doFilterInternal`), `AdminSeeder` (`run`).

### Outras do Java

| Anotação | Para que serve |
|---|---|
| `@FunctionalInterface` | Garante que a interface tem **um só** método abstrato (pode virar lambda) |
| `@Deprecated` | Marca como obsoleto; o compilador avisa quem usa |
| `@SuppressWarnings("unchecked")` | Silencia um aviso específico do compilador |
| `@SafeVarargs` | Declara seguro um método com varargs genéricos |

---

## 4. Aplicação e configuração do Spring Boot

### `@SpringBootApplication`
Fica na classe `main` de cada serviço. É um **atalho para três anotações**:

| Inclui | Faz |
|---|---|
| `@Configuration` | A classe pode declarar beans com `@Bean` |
| `@EnableAutoConfiguration` | O Boot configura sozinho o que encontra no classpath (ver o Postgres? configura DataSource; ver Web? sobe o Tomcat) |
| `@ComponentScan` | Varre o pacote da classe e os subpacotes procurando componentes |

Por isso a classe `main` precisa ficar no **pacote raiz**: o scan só enxerga dali para baixo.

### `@Configuration` e `@Bean`
Para criar beans **manualmente** (quando a classe não é sua ou precisa de configuração):

```java
@Configuration
public class RabbitMQConfig {

    @Bean                                           // o retorno vira um bean gerenciado
    public DirectExchange pedidosExchange() {
        return new DirectExchange("pedidos.exchange");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();   // serializa mensagens como JSON
    }
}
```

Usado também em `SecurityBeansConfig` (o `PasswordEncoder` BCrypt) e `SecurityConfig` (a cadeia de filtros).

### `@Value`
Injeta um valor do `application.yml` / variável de ambiente:

```java
public JwtService(@Value("${jwt.secret}") String secret,
                  @Value("${jwt.expiration-ms}") long expirationMs) { ... }
```

`${jwt.secret}` é lido de `application.yml`. Com valor padrão: `${JWT_COOKIE_SECURE:false}` (usa `false` se a variável não existir).

### `@Enable...` (ligam funcionalidades)

| Anotação | Liga |
|---|---|
| `@EnableEurekaServer` | Este app é o servidor de descoberta (`eureka-server`) |
| `@EnableDiscoveryClient` | Este app se registra/consulta o Eureka |
| `@EnableFeignClients` | Procura interfaces `@FeignClient` e gera as implementações |
| `@EnableWebSecurity` | Ativa a configuração customizada do Spring Security |

> Nas versões atuais do Spring Cloud, `@EnableDiscoveryClient` é **opcional**: basta ter o starter do Eureka no classpath. O projeto ainda a usa por clareza.

---

## 5. Estereótipos: @Component, @Service, @Repository...

Todas marcam "esta classe é um bean para o Spring gerenciar". As especializadas dizem **a camada** a que pertencem:

| Anotação | Camada | Faz algo a mais? |
|---|---|---|
| `@Component` | Genérica | Só registra o bean |
| `@Service` | Regras de negócio | Só semântica (documenta a intenção) |
| `@Repository` | Acesso a dados | Traduz exceções de persistência para as do Spring |
| `@Controller` | Web (retorna views HTML) | Registra endpoints |
| `@RestController` | Web/API REST | = `@Controller` + `@ResponseBody` (retorna JSON direto) |
| `@Configuration` | Configuração | Permite métodos `@Bean` |

Tecnicamente `@Service` e `@Component` fazem a mesma coisa. Usar a anotação certa **comunica a arquitetura** a quem lê e permite tratamentos específicos (como o do `@Repository`).

```
  Requisição ──► @RestController ──► @Service ──► @Repository ──► Banco
                 (HTTP)              (regras)      (dados)
```

Exemplos: `ProductController` (`@RestController`), `ProductService` (`@Service`), `ProductRepository` (`@Repository`), `AdminSeeder`/`JwtGlobalFilter` (`@Component`).

---

## 6. Injeção de dependência

Como o Spring entrega os beans:

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;   // final: nunca muda

    public OrderService(OrderRepository orderRepository) {   // o Spring chama este construtor
        this.orderRepository = orderRepository;
    }
}
```

Com **um único construtor**, o `@Autowired` é **desnecessário** (o Spring já sabe). Este projeto usa sempre **injeção por construtor**, que é a recomendada.

| Forma | Exemplo | Avaliação |
|---|---|---|
| **Construtor** | `public X(Y y) {...}` | ✅ Recomendada: campos `final`, fácil de testar, dependências explícitas |
| **Campo** | `@Autowired private Y y;` | ❌ Esconde dependências, difícil de testar sem Spring |
| **Setter** | `@Autowired void setY(Y y)` | Só para dependências opcionais |

Outras:

| Anotação | Uso |
|---|---|
| `@Autowired` | Pede injeção explícita (opcional em construtor único) |
| `@Qualifier("nome")` | Escolhe **qual** bean quando há mais de um do mesmo tipo |
| `@Primary` | Marca o bean padrão quando há vários do mesmo tipo |

---

## 7. Web e REST

### Mapeamento de rotas

```java
@RestController
@RequestMapping("/products")                    // prefixo de todas as rotas da classe
public class ProductController {

    @GetMapping("/{id}")                        // GET /products/5
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) { ... }

    @PostMapping                                // POST /products
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto dto) { ... }

    @DeleteMapping("/{id}")                     // DELETE /products/5
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) { ... }
}
```

| Anotação | Verbo HTTP | Uso típico |
|---|---|---|
| `@GetMapping` | GET | Ler |
| `@PostMapping` | POST | Criar / executar ação |
| `@PutMapping` | PUT | Substituir |
| `@PatchMapping` | PATCH | Alterar parte (`PATCH /orders/{id}/status`) |
| `@DeleteMapping` | DELETE | Remover |
| `@RequestMapping` | qualquer | Prefixo da classe ou mapeamento genérico |

### Dados da requisição

| Anotação | De onde vem o dado | Exemplo |
|---|---|---|
| `@PathVariable` | **Caminho** da URL | `/products/{id}` → `@PathVariable Long id` |
| `@RequestParam` | **Query string** | `/products/search?name=arroz` → `@RequestParam String name` |
| `@RequestBody` | **Corpo** (JSON → objeto) | `@RequestBody ProductRequestDto dto` |
| `@RequestHeader` | **Header** | `@RequestHeader("Authorization") String auth` |
| `@CookieValue` | **Cookie** | `@CookieValue("auth_token") String token` |

### `ResponseEntity`
Não é anotação, mas é o par delas: permite controlar **status, headers e corpo** da resposta (`ResponseEntity.ok(...)`, `.noContent()`, `HttpStatus.CREATED`). Detalhes em [HTTP-STATUS.md](HTTP-STATUS.md).

### `@ResponseStatus`
Alternativa para fixar o status direto no método ou na exceção: `@ResponseStatus(HttpStatus.CREATED)`. O projeto prefere `ResponseEntity` por ser mais explícito.

---

## 8. Tratamento de erros

```java
@RestControllerAdvice                                    // vale para TODOS os controllers
public class ProductExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)    // captura esta exceção (e suas filhas)
    public ResponseEntity<StandardError> handleNotFound(ProductNotFoundException ex,
                                                        HttpServletRequest request) {
        ...
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

| Anotação | Faz |
|---|---|
| `@RestControllerAdvice` | Classe que trata exceções de **todos** os controllers (= `@ControllerAdvice` + `@ResponseBody`) |
| `@ExceptionHandler(X.class)` | Método que trata a exceção `X` (e subclasses) |

Como funciona: o service lança uma exceção → o Spring percorre os `@ExceptionHandler` procurando o que melhor casa com o **tipo** → executa e devolve a resposta. Isso mantém os serviços livres de qualquer conhecimento de HTTP. Veja [HTTP-STATUS.md](HTTP-STATUS.md), seção 7.

---

## 9. Validação (Bean Validation)

Regras declaradas **no próprio DTO**:

```java
public record ProductRequestDto(
        @NotBlank(message = "Nome é obrigatório") String name,
        @NotBlank String sku,
        String description,
        @NotNull @DecimalMin(value = "0.0", message = "Preço deve ser maior ou igual a zero") BigDecimal price,
        @NotNull @Min(0) Integer stockQuantity
) {}
```

E ativadas no controller com **`@Valid`**:

```java
public ResponseEntity<...> create(@Valid @RequestBody ProductRequestDto dto) { ... }
```

Se uma regra falha, o Spring lança `MethodArgumentNotValidException` (que o handler converte em **400**) **antes** do método do controller executar.

| Anotação | Valida | Observação |
|---|---|---|
| `@NotNull` | Não é `null` | Aceita string vazia |
| `@NotEmpty` | Não é `null` nem vazio | Strings, listas |
| `@NotBlank` | Texto com algum caractere não-espaço | **Só para String** |
| `@Email` | Formato de e-mail | |
| `@Min(n)` / `@Max(n)` | Valor mínimo/máximo | Números inteiros |
| `@Positive` | Maior que zero | |
| `@DecimalMin("0.0")` | Mínimo para decimais | `BigDecimal` |
| `@Size(min, max)` | Tamanho de texto/lista | |
| `@Pattern(regexp)` | Casa com regex | |
| `@Valid` | **Dispara** a validação (e desce para objetos aninhados) | Ex.: lista de itens do pedido |

Diferença: `@NotNull` vs `@NotEmpty` vs `@NotBlank`: `null` / `""` / `"   "`. Apenas o `@NotBlank` recusa os três.

---

## 10. Persistência (JPA)

JPA mapeia **classes Java ↔ tabelas**. As anotações descrevem o mapeamento:

```java
@Entity                                   // esta classe é uma tabela
@Table(name = "products")                 // nome da tabela
public class Product {

    @Id                                   // chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // o banco gera (BIGSERIAL)
    private long id;

    @Column(length = 100, unique = true, nullable = false)  // coluna com restrições
    private String sku;

    @Column(name = "stock_quantity", nullable = false)      // nome diferente da coluna
    private int stockQuantity;
}
```

| Anotação | Faz |
|---|---|
| `@Entity` | Marca a classe como entidade persistente (precisa de construtor vazio e `@Id`) |
| `@Table(name=...)` | Nome da tabela |
| `@Id` | Chave primária |
| `@GeneratedValue` | Como o ID é gerado (`IDENTITY` = autoincremento do banco) |
| `@Column` | Detalhes da coluna: nome, tamanho, `nullable`, `unique`, `precision`/`scale` |
| `@Enumerated(EnumType.STRING)` | Grava o enum pelo **nome** (use sempre `STRING`; `ORDINAL` quebra se reordenar) |

### Relacionamentos (`Order` ↔ `OrderItem`)

```java
// Order.java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
private List<OrderItem> items = new ArrayList<>();

// OrderItem.java
@ManyToOne
@JoinColumn(name = "order_id")
private Order order;
```

| Anotação | Significado |
|---|---|
| `@OneToMany` | Um pedido tem **vários** itens |
| `@ManyToOne` | Vários itens pertencem a **um** pedido |
| `@JoinColumn` | A coluna de chave estrangeira (`order_id`) |
| `mappedBy` | "O dono do relacionamento é o campo `order` do outro lado" |
| `cascade = ALL` | Salvar/apagar o pedido propaga para os itens |

Nota de arquitetura: **não há relacionamento JPA entre serviços** (`Order` guarda só `customerId`), porque cada serviço tem seu próprio banco.

### Consultas personalizadas

```java
@Modifying(clearAutomatically = true)                         // é um UPDATE/DELETE, não um SELECT
@Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
       "WHERE p.id = :id AND p.stockQuantity >= :quantity")   // JPQL (usa nomes de classe/campo)
int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
```

| Anotação | Faz |
|---|---|
| `@Query` | Define a consulta (JPQL ou SQL nativo) |
| `@Modifying` | Avisa que a consulta **altera** dados; o retorno `int` é o nº de linhas afetadas |
| `@Param` | Liga `:nome` da consulta ao parâmetro do método |

Métodos como `existsBySku(String sku)` **não precisam de anotação**: o Spring Data **deduz a consulta pelo nome** do método.

---

## 11. Transações: @Transactional

Uma **transação** garante "tudo ou nada": várias operações no banco ou **todas** são confirmadas (*commit*) ou **todas** são desfeitas (*rollback*).

```java
@Transactional
public OrderResponseDto createOrder(OrderRequestDto dto) {
    ...
    orderRepository.save(order);     // se algo falhar depois, o save é desfeito
    ...
}
```

Como funciona: o Spring cria um **proxy** em volta do `OrderService`. Ao chamar `createOrder`, o proxy **abre** a transação, executa o método e: se terminou bem, **commit**; se lançou exceção, **rollback**.

**Regras que pegam todo mundo**

| Regra | Detalhe |
|---|---|
| Rollback automático | Só para **`RuntimeException`** (não verificadas) e `Error`. Exceção verificada **não** desfaz por padrão (`rollbackFor = Exception.class` muda isso) |
| **Self-invocation** | Se um método chama **outro da mesma classe** (`this.outro()`), o `@Transactional` do outro **é ignorado**: a chamada não passa pelo proxy |
| Visibilidade | Só funciona em métodos **públicos** chamados **de fora** da classe |
| Só vale para o **banco** | Chamadas HTTP e mensagens enviadas **não** são desfeitas pelo rollback |

O último ponto é central neste projeto: o `order-service` baixa o estoque no `product-service` por HTTP, e **o rollback do banco não desfaz isso**. Por isso o código faz a **compensação manual** (devolver o estoque no `catch`). Esse é o problema clássico de transações distribuídas.

Outros atributos úteis: `@Transactional(readOnly = true)` (otimização para consultas), `propagation`, `isolation`.

---

## 12. Spring Cloud: Eureka e Feign

### `@FeignClient`: cliente HTTP declarativo

```java
@FeignClient(name = "product-service")          // nome registrado no Eureka (sem URL fixa!)
public interface ProductClient {

    @GetMapping("/products/{id}")               // as MESMAS anotações do Spring MVC
    ProductDto getProductById(@PathVariable Long id);

    @PostMapping("/products/{id}/stock/decrease")
    ProductDto decreaseStock(@PathVariable Long id, @RequestBody StockAdjustmentDto dto);
}
```

Você escreve **só a interface**; o Feign gera a classe que monta a URL, faz a chamada HTTP e converte o JSON. O `name = "product-service"` é resolvido pelo **Eureka** + load balancer. Erros HTTP viram `FeignException` (`FeignException.NotFound` para 404, `Conflict` para 409...).

### Eureka

| Anotação | Papel |
|---|---|
| `@EnableEurekaServer` | `eureka-server`: o catálogo de serviços |
| `@EnableDiscoveryClient` | Os demais: registram-se e consultam o catálogo |

---

## 13. Resiliência: Resilience4j

```java
@CircuitBreaker(name = "customerService", fallbackMethod = "customerFallback")
@TimeLimiter(name = "customerService")
public CompletableFuture<CustomerDto> getCustomer(Long customerId) { ... }
```

| Anotação | Faz |
|---|---|
| `@CircuitBreaker` | Se muitas chamadas falham, **abre o circuito**: passa a recusar de imediato (sem tentar a rede) por um tempo, protegendo o sistema |
| `@TimeLimiter` | Aborta a chamada se passar do tempo (3 s aqui). Exige retorno `CompletableFuture` |
| `fallbackMethod` | Método chamado quando falha (mesma assinatura + `Throwable`) |

A configuração dos limites está em `application.yml` (`resilience4j.circuitbreaker...`). Como `@Transactional`, são **proxies**: sofrem de **self-invocation**. Por isso `ResilientExternalServiceClient` é uma **classe separada** do `OrderService`.

---

## 14. Mensageria: RabbitMQ

```java
@Component
public class PedidoCriadoConsumer {

    @RabbitListener(queues = "pedido.criado.queue")        // chamado a cada mensagem da fila
    public void receberPedidoCriado(Map<String, Object> evento) { ... }
}
```

`@RabbitListener` registra o método como **consumidor**: o Spring fica escutando a fila e invoca o método a cada mensagem, convertendo o JSON no parâmetro. O produtor usa o `RabbitTemplate` (sem anotação), e a infraestrutura (exchange, fila, binding) é declarada com `@Bean` na `RabbitMQConfig`.

---

## 15. Segurança e JSON

| Anotação | Faz |
|---|---|
| `@EnableWebSecurity` | Habilita a configuração do Spring Security (`SecurityConfig`) |
| `@JsonFormat(pattern = "dd/MM/yyyy - HH:mm:ss")` | Define como uma data é escrita no JSON (usada no `StandardError`) |
| `@JsonProperty("nome")` | Renomeia um campo no JSON |
| `@JsonIgnore` | Esconde um campo do JSON (ex.: senha) |

Dica de segurança: DTOs de **resposta** nunca devem conter a senha. Neste projeto o `UserResponseDTO` simplesmente **não tem** esse campo (melhor que depender de `@JsonIgnore`).

---

## 16. Anotações de teste

Detalhadas em [TESTES.md](TESTES.md). Resumo:

| Anotação | Faz |
|---|---|
| `@Test` | Marca um método como teste (JUnit) |
| `@ExtendWith(MockitoExtension.class)` | Liga o Mockito ao JUnit |
| `@Mock` | Cria um dublê (falso) da dependência |
| `@InjectMocks` | Cria a classe real sob teste e injeta os `@Mock` nela |
| `@Tag("integration")` | Rotula o teste para incluir/excluir grupos |
| `@SpringBootTest` | Sobe a aplicação Spring inteira (pesado; teste de integração) |
| `@Testcontainers` + `@Container` | Gerencia um container Docker durante o teste |
| `@ServiceConnection` | Conecta o Spring automaticamente ao container |
| `@WebMvcTest` | Sobe só a camada web (controllers) |
| `@DataJpaTest` | Sobe só a camada JPA |
| `@MockitoBean` | Substitui um bean do contexto Spring por um mock |

---

## 17. Mapa: onde cada anotação aparece no projeto

Contagem aproximada no código de produção (`src/main`):

| Grupo | Anotações | Onde |
|---|---|---|
| **Estereótipos** | `@Service` (8), `@RestController` (5), `@Component` (4), `@Repository` (3), `@Configuration` (4), `@Bean` (10) | Camadas de todos os serviços |
| **Aplicação** | `@SpringBootApplication` (7), `@EnableDiscoveryClient` (4), `@EnableEurekaServer`, `@EnableFeignClients`, `@EnableWebSecurity` | Classes `main` e config |
| **Config** | `@Value` (8) | JWT, cookie, senha do admin |
| **Web** | `@GetMapping` (15), `@PostMapping` (10), `@PutMapping` (3), `@DeleteMapping` (3), `@PatchMapping` (1), `@RequestMapping` (5) | Controllers |
| **Erros** | `@RestControllerAdvice` (4), `@ExceptionHandler` (17) | `*ExceptionHandler` |
| **Validação** | `@NotBlank` (9), `@NotNull` (6), `@Valid` (2), `@Positive`, `@NotEmpty`, `@Min`, `@Email`, `@DecimalMin` | DTOs |
| **JPA** | `@Column` (22), `@Entity` (5), `@Table` (5), `@Id` (5), `@GeneratedValue` (5), `@OneToMany`, `@ManyToOne`, `@JoinColumn`, `@Enumerated`, `@Query` (2), `@Modifying` (2) | Models e repositórios |
| **Transação** | `@Transactional` (7) | `UserService`, `ProductService`, `OrderService` |
| **Cloud** | `@FeignClient` (2) | `CustomerClient`, `ProductClient` |
| **Resiliência** | `@CircuitBreaker` (2), `@TimeLimiter` (2) | `ResilientExternalServiceClient` |
| **Mensageria** | `@RabbitListener` (1) | `PedidoCriadoConsumer` |
| **JSON** | `@JsonFormat` (4) | `StandardError` |
| **Java** | `@Override` (4) | Filtros e `AdminSeeder` |

---

## 18. Armadilhas comuns

| Armadilha | Sintoma | Solução |
|---|---|---|
| Esquecer `@Valid` no `@RequestBody` | As validações do DTO **não rodam** | Sempre `@Valid @RequestBody` |
| Chamar método `@Transactional`/`@CircuitBreaker` da **mesma classe** | A anotação é ignorada (self-invocation) | Mover para outra classe/bean |
| Classe `main` fora do pacote raiz | Beans não encontrados | Manter a `@SpringBootApplication` no pacote mais externo |
| `@Service` em classe que você instancia com `new` | O objeto **não** é gerenciado: sem injeção, sem proxy | Deixar o Spring injetar |
| Dois beans do mesmo tipo | `NoUniqueBeanDefinitionException` | `@Qualifier` ou `@Primary` |
| `@Enumerated(ORDINAL)` | Reordenar o enum corrompe os dados | Usar `EnumType.STRING` |
| `@Transactional` com exceção verificada | Não faz rollback | `rollbackFor` ou usar `RuntimeException` |
| `@NotBlank` em `Integer` | Erro em tempo de execução | Usar `@NotNull` para não-texto |
| `@Entity` sem construtor vazio | O Hibernate não consegue instanciar | Adicionar construtor padrão |
| Esperar que o rollback desfaça chamadas HTTP | Estado inconsistente entre serviços | Compensação manual / saga |
| Acreditar que a anotação "faz" algo sozinha | Nada acontece (sem Spring lendo) | Lembrar: anotação = etiqueta; alguém precisa lê-la |

---

## 19. Como criar sua própria anotação

Para entender de verdade, veja como é uma anotação por dentro:

```java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)          // ainda existe em tempo de execução (para reflexão ler)
@Target(ElementType.METHOD)                  // só pode ser colocada em métodos
public @interface LogExecutionTime {
    String value() default "";               // parâmetro opcional
}
```

Uso:

```java
@LogExecutionTime("criação de pedido")
public OrderResponseDto createOrder(...) { ... }
```

Sozinha, ela **não faz nada**. Para dar efeito, alguém precisa ler. No Spring isso se faz com **AOP** (programação orientada a aspectos):

```java
@Aspect @Component
public class LogTimeAspect {
    @Around("@annotation(logExecutionTime)")           // intercepta métodos com a anotação
    public Object medir(ProceedingJoinPoint pjp, LogExecutionTime logExecutionTime) throws Throwable {
        long inicio = System.currentTimeMillis();
        try {
            return pjp.proceed();                      // executa o método original
        } finally {
            System.out.println(logExecutionTime.value() + ": " + (System.currentTimeMillis() - inicio) + " ms");
        }
    }
}
```

É exatamente assim que `@Transactional` e `@CircuitBreaker` funcionam: uma anotação + um aspecto/proxy que a lê e **embrulha** a chamada.

`@Retention` define até quando a anotação vive: `SOURCE` (só no código-fonte), `CLASS` (no `.class`, mas não em execução) e `RUNTIME` (em execução, a que os frameworks usam).

---

## 20. Perguntas de entrevista

**O que é uma anotação e como o Spring a usa?**
Metadado sobre o código. O Spring a lê em tempo de execução (reflexão) para registrar beans, mapear rotas, criar proxies etc.

**Diferença entre `@Component`, `@Service` e `@Repository`?**
Todos registram um bean. `@Service` e `@Repository` indicam a camada; `@Repository` ainda traduz exceções de persistência.

**O que faz `@SpringBootApplication`?**
Agrupa `@Configuration`, `@EnableAutoConfiguration` e `@ComponentScan`.

**Por que preferir injeção por construtor?**
Campos `final`, dependências explícitas, objeto sempre válido e fácil de instanciar em testes sem Spring.

**O que é `@Transactional` e quando ele NÃO funciona?**
Abre/commita/desfaz transação do banco via proxy. Não funciona em chamadas da mesma classe (self-invocation), em métodos não públicos, e só reverte exceções não verificadas por padrão.

**Diferença entre `@PathVariable`, `@RequestParam` e `@RequestBody`?**
Caminho da URL, query string e corpo da requisição.

**`@RestController` vs `@Controller`?**
`@RestController` = `@Controller` + `@ResponseBody` (devolve dados, tipicamente JSON, e não uma view HTML).

**Para que serve `@Override`?**
Faz o compilador garantir que o método realmente sobrescreve algo.

**Como o Spring Data cria um repositório só com uma interface?**
Gera, em tempo de execução, uma implementação (proxy) a partir do tipo `JpaRepository<T, ID>` e da leitura dos nomes e anotações dos métodos.

**O que é um bean?**
Objeto criado e gerenciado pelo contêiner do Spring.

---

## 21. Exercícios

1. **Detetive.** Em `OrderService`, liste todas as anotações que "agem" sobre a classe, direta ou indiretamente (dica: o que o Spring faz com `@Service` e `@Transactional`?).
2. **Self-invocation.** Em `ResilientExternalServiceClient`, mova `@CircuitBreaker` para um método chamado por outro método da **mesma** classe. Provoque a falha e observe que o circuito não abre. Desfaça.
3. **Validação.** Adicione `@Size(max = 100)` ao nome do `CustomerRequestDto` e escreva (com `@WebMvcTest`) um teste que envie 101 caracteres e espere **400**.
4. **Sua anotação.** Implemente `@LogExecutionTime` da seção 19 (precisa do starter `spring-boot-starter-aspectj`, que o `order-service` já tem) e aplique-a ao `createOrder`.
5. **Mapeie.** Para o endpoint `POST /orders`, descreva em ordem as anotações que entram em ação desde a requisição até a resposta (`@RestController`, `@PostMapping`, `@Valid`, `@RequestBody`, `@Transactional`, `@CircuitBreaker`, `@ExceptionHandler`...).
6. **Enum.** Troque temporariamente `@Enumerated(EnumType.STRING)` por `ORDINAL` no `User` e descreva por que reordenar o `UserRole` corromperia os dados existentes.
