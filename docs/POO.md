# Orientação a Objetos em Java: classes, herança, polimorfismo, interfaces e quando usar cada um

Guia de estudo. Os exemplos vêm do código real deste projeto.

## Índice

1. [A ideia central](#1-a-ideia-central)
2. [Classes e objetos](#2-classes-e-objetos)
3. [Encapsulamento](#3-encapsulamento)
4. [Herança](#4-herança)
5. [Classes abstratas](#5-classes-abstratas)
6. [Interfaces](#6-interfaces)
7. [Polimorfismo](#7-polimorfismo)
8. [Interface vs. classe abstrata vs. herança: quando usar cada um](#8-interface-vs-classe-abstrata-vs-herança-quando-usar-cada-um)
9. [Composição: a alternativa preferida à herança](#9-composição-a-alternativa-preferida-à-herança)
10. [Records, enums e genéricos](#10-records-enums-e-genéricos)
11. [Como tudo isso aparece no Spring](#11-como-tudo-isso-aparece-no-spring)
12. [Princípios SOLID em resumo](#12-princípios-solid-em-resumo)
13. [Pontos de melhoria no projeto (para praticar)](#13-pontos-de-melhoria-no-projeto-para-praticar)
14. [Perguntas de entrevista](#14-perguntas-de-entrevista)
15. [Exercícios](#15-exercícios)

---

## 1. A ideia central

Programação Orientada a Objetos (POO) organiza o código em **objetos**: unidades que juntam **dados** (estado) e **comportamento** (métodos). Em vez de funções soltas manipulando dados soltos, você modela o problema com "coisas" do domínio: `Order`, `Product`, `Customer`.

Os **4 pilares**:

| Pilar | Em uma frase | Exemplo no projeto |
|---|---|---|
| **Encapsulamento** | Esconder os detalhes internos e expor só o necessário | Campos `private` em `Product`, acessados por getters/setters |
| **Abstração** | Mostrar *o quê*, esconder *como* | `ProductRepository` (você chama `save`, não escreve SQL) |
| **Herança** | Uma classe reaproveita e especializa outra ("é um") | `ProductNotFoundException extends RuntimeException` |
| **Polimorfismo** | Tratar tipos diferentes pela mesma interface | `PasswordEncoder` pode ser BCrypt, ou um mock no teste |

---

## 2. Classes e objetos

- **Classe** = a planta/molde (define campos e métodos).
- **Objeto (instância)** = uma casa construída a partir da planta (`new`).

```java
// A classe: o molde
@Entity
public class Order {
    private Long customerId;          // estado (campos)
    private String status;
    private BigDecimal total;

    public Order(Long customerId, String status) {   // construtor: como criar
        this.customerId = customerId;
        this.status = status;
    }

    public BigDecimal getTotal() { return total; }   // comportamento (métodos)
}

// Os objetos: instâncias independentes, cada uma com seu próprio estado
Order pedidoA = new Order(1L, "CREATED");
Order pedidoB = new Order(2L, "CONFIRMED");
```

**Conceitos básicos**

| Conceito | O que é |
|---|---|
| **Campo (atributo)** | Variável que guarda o estado do objeto |
| **Método** | Função que define o comportamento |
| **Construtor** | Código que roda no `new` para inicializar o objeto. Tem o nome da classe e não tem tipo de retorno |
| **`this`** | Referência ao próprio objeto (`this.customerId = customerId` distingue o campo do parâmetro) |
| **`static`** | Pertence à **classe**, não a uma instância (`Jwts.builder()`, `BigDecimal.ZERO`) |
| **`final`** | Não pode ser reatribuído (campo), sobrescrito (método) ou herdado (classe) |

**Modificadores de acesso**

| Modificador | Quem enxerga |
|---|---|
| `private` | Só a própria classe |
| *(nenhum / package-private)* | Classes do mesmo pacote |
| `protected` | Mesmo pacote e subclasses |
| `public` | Todo mundo |

Regra prática: **comece com o acesso mais restrito possível** (`private`) e só abra quando precisar.

**Sobrecarga (overloading)**: vários métodos com o **mesmo nome e parâmetros diferentes** na mesma classe. Não confunda com sobrescrita (veja a seção 7).

---

## 3. Encapsulamento

Esconder o estado e controlar o acesso a ele. Em vez de qualquer código mudar `order.status = "BANANA"`, o acesso passa por métodos:

```java
public class Product {
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;                    // ninguém mexe direto

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
}
```

**Por que isso importa**
- Você pode **validar** na entrada (um setter poderia recusar estoque negativo).
- Pode **mudar a implementação interna** sem quebrar quem usa a classe.
- Evita estados inválidos espalhados pelo sistema.

> Getters/setters sem nenhuma regra (como acima) são só "encapsulamento de fachada". O ganho real vem quando o objeto **protege suas próprias regras**. Exemplo no projeto: `OrderStatus.canTransitionTo(...)` mantém a regra "quem pode ir para onde" **dentro do enum**, e não espalhada pelos serviços.

### Imutabilidade: o encapsulamento mais forte

Um objeto **imutável** nunca muda depois de criado. Não há setter, então não há estado inválido nem problema de concorrência.

```java
// record: imutável por construção. Os DTOs do projeto são todos assim.
public record LoginRequestDTO(@NotBlank String username, @NotBlank String password) {}

// campo final + injeção por construtor: a dependência nunca é trocada depois
public class OrderService {
    private final OrderRepository orderRepository;
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

---

## 4. Herança

Uma classe (**subclasse/filha**) herda campos e métodos de outra (**superclasse/mãe**) com `extends`. A relação é **"é um"** (*is-a*): uma `ProductNotFoundException` **é uma** `RuntimeException`.

```java
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);            // chama o construtor da mãe
    }
}
```

O que se ganha: `getMessage()`, `printStackTrace()`, o comportamento de exceção não verificada e a capacidade de ser lançada com `throw`, **sem escrever nada disso**.

**Hierarquia de exceções do Java** (herança em ação):

```
Throwable
├── Error                      (problemas da JVM; não se trata)
└── Exception
    ├── (verificadas: IOException, ExecutionException...)  → o compilador obriga tratar
    └── RuntimeException       → NÃO verificadas
        ├── IllegalArgumentException
        ├── ProductNotFoundException        ← do projeto
        ├── InsufficientStockException      ← do projeto
        └── ...
```

Por que as exceções do projeto estendem `RuntimeException`? Porque erros de regra de negócio não devem forçar `try/catch` em toda chamada, e o `@Transactional` do Spring só faz rollback automático em exceções não verificadas.

**`super`**: refere-se à superclasse. `super(message)` chama o construtor dela; `super.metodo()` chama a versão herdada de um método.

### Sobrescrita (override)

A subclasse **redefine** um método herdado. A anotação `@Override` pede ao compilador para conferir que você realmente está sobrescrevendo (evita erro de digitação no nome ou nos parâmetros):

```java
// JwtGlobalFilter implementa a interface Ordered
@Override
public int getOrder() {
    return -1;      // roda antes dos outros filtros
}
```

### Quando a herança dá problema

| Problema | Explicação |
|---|---|
| **Acoplamento forte** | A filha depende dos detalhes da mãe. Mudar a mãe pode quebrar todas as filhas |
| **Herança única** | Em Java uma classe só pode estender **uma** classe |
| **Hierarquias profundas** | Difíceis de entender ("onde esse método está definido?") |
| **"É um" mentiroso** | Herdar só para reaproveitar código, sem relação real, gera modelos errados (um `Quadrado extends Retangulo` quebra regras do retângulo) |

Teste mental: **"X é um Y em todos os contextos?"** Se a resposta não for um "sim" óbvio, não use herança.

---

## 5. Classes abstratas

Uma classe `abstract` **não pode ser instanciada** (`new`). Ela serve de base: traz código pronto **e** deixa métodos **abstratos** (sem corpo) que as filhas são obrigadas a implementar.

Exemplo real no projeto: `JwtAuthenticationFilter extends OncePerRequestFilter`.

```java
// Spring (simplificado): a classe abstrata define o "esqueleto" do algoritmo
public abstract class OncePerRequestFilter {
    public final void doFilter(...) {
        // garante que o filtro rode UMA vez por requisição (código pronto)
        doFilterInternal(request, response, chain);   // ← o "buraco" que você preenche
    }
    protected abstract void doFilterInternal(...);
}

// Seu código: preenche só a parte específica
@Override
protected void doFilterInternal(HttpServletRequest request, ...) {
    String token = extractToken(request);
    ...
}
```

Isso é o padrão **Template Method**: a classe mãe controla o fluxo geral; a filha só implementa os passos que variam. É um uso saudável de herança: a mãe foi **projetada para ser estendida**.

---

## 6. Interfaces

Uma interface é um **contrato**: lista **o que** uma classe sabe fazer, sem dizer **como**.

```java
// A interface: só assinaturas
public interface PasswordEncoder {
    String encode(CharSequence rawPassword);
    boolean matches(CharSequence rawPassword, String encodedPassword);
}

// Uma implementação concreta
public class BCryptPasswordEncoder implements PasswordEncoder { ... }
```

Quem usa **depende da interface**, não da implementação:

```java
public class AuthService {
    private final PasswordEncoder passwordEncoder;   // não sabe que é BCrypt
    ...
}
```

### Interfaces que aparecem no projeto

| Interface | Quem implementa | Para quê |
|---|---|---|
| `JpaRepository<T, ID>` | **Ninguém no seu código**: o Spring gera a classe em tempo de execução | `ProductRepository`, `OrderRepository`... |
| `@FeignClient` (interfaces) | O Feign gera a implementação HTTP | `CustomerClient`, `ProductClient` |
| `GlobalFilter` + `Ordered` | `JwtGlobalFilter` | Filtro do Gateway (**duas** interfaces ao mesmo tempo) |
| `CommandLineRunner` | `AdminSeeder` | Código que roda ao subir a aplicação |
| `PasswordEncoder` | `BCryptPasswordEncoder` | Hash de senha |

Veja `ProductRepository`: **você só escreve a interface**, e o Spring cria a implementação:

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySku(String sku);              // o Spring deduz o SQL pelo nome do método
}
```

### Regras e recursos das interfaces

- Uma classe pode **implementar várias** interfaces (`implements GlobalFilter, Ordered`), ao contrário da herança de classe (uma só).
- Uma interface pode **estender outras** (`extends`).
- Métodos são `public abstract` por padrão.
- **`default` methods** (Java 8+): corpo padrão dentro da interface, que as implementações podem aproveitar ou sobrescrever.
- **Interface funcional**: tem **um único método abstrato** e pode ser escrita como **lambda**. Exemplo: `argThat(u -> u.getPassword().equals("hash"))` nos testes.
- **Não guarda estado de instância** (só constantes).

---

## 7. Polimorfismo

*Poli* (muitas) + *morfos* (formas): **um mesmo código funciona com objetos de tipos diferentes**, cada um respondendo à sua maneira.

### 7.1 Polimorfismo de subtipo (o principal)

Uma variável do tipo da mãe/interface pode apontar para qualquer filha/implementação:

```java
PasswordEncoder encoder = new BCryptPasswordEncoder();   // tipo da interface, objeto concreto
encoder.encode("senha");                                  // executa a versão do BCrypt
```

A JVM decide **em tempo de execução** qual método chamar, de acordo com o **objeto real** (*dynamic dispatch*, ou ligação dinâmica).

**É o que torna os testes unitários possíveis.** No `AuthServiceTest`:

```java
@Mock private PasswordEncoder passwordEncoder;   // um "falso" PasswordEncoder
@InjectMocks private AuthService authService;     // AuthService nem percebe a troca
```

O `AuthService` usa um `PasswordEncoder`. Em produção é BCrypt; no teste é um mock. **O código de produção é idêntico nos dois casos**, e isso só funciona porque ele depende da **interface**.

### 7.2 Capturar pela superclasse (hierarquia de exceções)

No `OrderService.decreaseStock`:

```java
try {
    productClient.decreaseStock(...);
} catch (FeignException.Conflict e) {         // 409  (mais específica)
    throw new InsufficientStockException(...);
} catch (FeignException.NotFound e) {         // 404  (mais específica)
    throw new ProductNotFoundException(...);
} catch (FeignException e) {                  // qualquer outra (a mãe)
    throw new ProductNotFoundException("Product service unavailable...");
}
```

`FeignException.Conflict` **é uma** `FeignException`. A **ordem importa**: do mais específico para o mais genérico. Se `catch (FeignException e)` viesse primeiro, os outros nunca seriam alcançados (o compilador acusa erro).

### 7.3 Sobrecarga vs. sobrescrita

| | Sobrecarga (*overload*) | Sobrescrita (*override*) |
|---|---|---|
| O que é | Mesmo nome, **parâmetros diferentes** | Mesma assinatura, **comportamento novo na filha** |
| Onde | Na **mesma classe** | Em **classe filha / implementação** |
| Resolvida | Na **compilação** | Na **execução** (polimorfismo) |
| Marca | nada | `@Override` |

### 7.4 Polimorfismo com enum

O `OrderStatus` mostra comportamento diferente por constante, sem `if/else` espalhado pelo sistema:

```java
public Set<OrderStatus> allowedNext() {
    return switch (this) {
        case CREATED   -> Set.of(CONFIRMED, CANCELED);
        case CONFIRMED -> Set.of(SHIPPED, CANCELED);
        case SHIPPED   -> Set.of(DELIVERED);
        case DELIVERED, CANCELED -> Set.of();
    };
}
```

---

## 8. Interface vs. classe abstrata vs. herança: quando usar cada um

### Comparação

| | **Interface** | **Classe abstrata** | **Classe concreta (herança)** |
|---|---|---|---|
| Expressa | "**Sabe fazer**" (capacidade/contrato) | "**É um tipo de**" com código base compartilhado | "**É um**" especializado |
| Quantas posso usar | **Várias** (`implements A, B`) | **Uma** (`extends`) | **Uma** (`extends`) |
| Código pronto | Só `default` (limitado) | Sim, à vontade | Sim |
| Estado (campos) | Não (só constantes) | **Sim** | Sim |
| Construtor | Não | Sim | Sim |
| Instanciável | Não | Não | Sim |
| Acoplamento | **Baixo** | Médio | **Alto** |

### Guia de decisão

```
Preciso de um contrato que várias classes distintas cumpram,
sem compartilhar código/estado?                         ──► INTERFACE

Várias classes são variações do mesmo conceito,
compartilham código E ESTADO, e há um "esqueleto" comum? ──► CLASSE ABSTRATA

Quero só reaproveitar código de outra classe que NÃO
é "do mesmo tipo" que a minha?                          ──► COMPOSIÇÃO (não herança)

É um "é um" real, estável, e a mãe foi feita para isso?  ──► HERANÇA (com cuidado)
```

### Quando usar INTERFACE
- Para **desacoplar** (depender de abstração): `PasswordEncoder`, `ProductClient`.
- Quando classes **sem parentesco** compartilham uma capacidade (`Comparable`, `Runnable`).
- Quando você quer **trocar implementações** (BCrypt ↔ Argon2; banco real ↔ fake no teste).
- Quando precisa de **múltiplos contratos** (`GlobalFilter` + `Ordered`).
- **Para testar**: interfaces são fáceis de simular com mocks.

### Quando usar CLASSE ABSTRATA
- Há um **algoritmo com passos fixos e passos variáveis** (Template Method: `OncePerRequestFilter`).
- As filhas **compartilham estado** (campos) e comportamento real, não só assinaturas.
- Você quer **forçar** um esqueleto comum e impedir a instanciação da base.

### Quando usar HERANÇA
- Relação "é um" **genuína** e **estável**.
- A classe mãe foi **desenhada para extensão** (documentada, com ganchos claros).
- Exemplo bom: exceções (`extends RuntimeException`).
- Exemplo ruim: estender uma classe só para pegar um método utilitário. Use composição.

### Exemplos do projeto lado a lado

| Situação | Escolha | Por quê |
|---|---|---|
| `ProductRepository` | **Interface** | Contrato; o Spring fornece a implementação |
| `JwtGlobalFilter` | **Interface** (duas) | Cumpre dois contratos independentes (`GlobalFilter`, `Ordered`) |
| `JwtAuthenticationFilter` | **Classe abstrata** (herança) | `OncePerRequestFilter` traz o esqueleto pronto (Template Method) |
| `ProductNotFoundException` | **Herança** | "É uma" `RuntimeException`, relação natural |
| `OrderService` usando `ProductClient` | **Composição** | O serviço **tem** um client; não **é** um client |

---

## 9. Composição: a alternativa preferida à herança

> **"Prefira composição à herança."** (Effective Java / Gang of Four)

**Composição** = um objeto **tem** outros objetos e delega trabalho a eles ("tem um", *has-a*).

```java
public class OrderService {                       // NÃO herda de nada
    private final OrderRepository orderRepository;        // tem um repositório
    private final ProductClient productClient;            // tem um client
    private final RabbitTemplate rabbitTemplate;          // tem um publicador

    public OrderService(OrderRepository orderRepository,
                        ProductClient productClient,
                        RabbitTemplate rabbitTemplate) { ... }   // injeção de dependência
}
```

Se `OrderService extends ProductClient`, seria absurdo: um pedido **não é** um cliente HTTP. Ele **usa** um.

**Vantagens da composição**
- Trocar a peça por outra sem mexer na classe (injeção).
- Testar substituindo as dependências por mocks.
- Combinar várias capacidades (herança só permite uma mãe).
- Menos acoplamento: depende do contrato, não dos detalhes internos de uma mãe.

**Injeção de Dependência (DI)** é a forma de fazer composição no Spring: você declara o que precisa **no construtor** e o Spring entrega. Isso é o **Princípio da Inversão de Dependência** na prática: o `OrderService` não faz `new ProductClient()`, ele **recebe** um.

---

## 10. Records, enums e genéricos

### Records (Java 16+)
Classe **imutável e enxuta** para carregar dados. O compilador gera construtor, getters (`nome()`), `equals`, `hashCode` e `toString`.

```java
public record PedidoCriadoEvent(Long orderId, Long customerId, BigDecimal total) {}
// uso:  event.orderId()   (e não getOrderId())
```

Use para **DTOs, eventos e respostas**. Neste projeto todo DTO é record. **Não** use como entidade JPA (precisa de construtor padrão e campos mutáveis).

### Enums
Conjunto **fixo** de valores, com possibilidade de ter métodos. `UserRole` (`ADMIN`, `GERENTE`...) e `OrderStatus` (com regras de transição). Melhor que `String` ou `int`: o compilador impede valores inválidos. No banco, `@Enumerated(EnumType.STRING)` grava o **nome** (e não a posição, que quebraria ao reordenar).

### Genéricos
Permitem reaproveitar código **mantendo a segurança de tipos**:

```java
public interface UserRepository extends JpaRepository<User, Long> { }
//                                       entidade ──┘     └── tipo do ID
```

`JpaRepository<T, ID>` serve para qualquer entidade: o `T` é preenchido por quem usa. Sem genéricos haveria uma interface por entidade, ou `Object` por todo lado com casts inseguros.

---

## 11. Como tudo isso aparece no Spring

O Spring é construído sobre POO, e entender isso desmistifica a "mágica":

| Mecanismo do Spring | Conceito de POO por trás |
|---|---|
| Injeção de dependência | Composição + interfaces (inversão de dependência) |
| `ProductRepository` sem implementação | Interface + **proxy dinâmico** gerado em tempo de execução |
| `@FeignClient` em interface | Idem: o Feign gera a classe que faz HTTP |
| `@Transactional`, `@CircuitBreaker` | **Proxies** que "embrulham" seu objeto (polimorfismo: o chamador acha que fala com o seu serviço, mas fala com um proxy do mesmo tipo) |
| `@RestControllerAdvice` + `@ExceptionHandler` | Captura por **tipo da exceção**, usando a hierarquia de herança |
| `OncePerRequestFilter` | Classe abstrata + Template Method |
| `CommandLineRunner`, `GlobalFilter` | Interfaces: você implementa o contrato e o Spring chama |

**Por que as chamadas internas ignoram `@Transactional`/`@CircuitBreaker`?** Porque o proxy envolve o objeto **por fora**. Se um método chama outro **da mesma classe** (`this.metodo()`), a chamada não passa pelo proxy. Foi por isso que o `ResilientExternalServiceClient` virou uma classe separada do `OrderService`. Mais sobre isso em [ANOTACOES.md](ANOTACOES.md).

---

## 12. Princípios SOLID em resumo

| Letra | Princípio | Em uma frase | No projeto |
|---|---|---|---|
| **S** | Responsabilidade Única | Uma classe, um motivo para mudar | `JwtService` só cuida de token; `AuthCookieService` só do cookie |
| **O** | Aberto/Fechado | Aberta para extensão, fechada para modificação | Novo handler de exceção sem mexer nos existentes |
| **L** | Substituição de Liskov | Uma filha deve poder substituir a mãe sem quebrar nada | Qualquer `PasswordEncoder` funciona no `AuthService` |
| **I** | Segregação de Interface | Interfaces pequenas e focadas | `Ordered` e `GlobalFilter` separadas |
| **D** | Inversão de Dependência | Dependa de abstrações, não de concretos | Serviços recebem interfaces por construtor |

---

## 13. Pontos de melhoria no projeto (para praticar)

Estes são casos reais de código repetido que a POO resolveria.

**1. Exceções "NotFound" repetidas.** Existem `CustomerNotFoundException`, `ProductNotFoundException`, `OrderNotFoundException`, `UserNotFoundException`... todas idênticas. Uma hierarquia as unificaria:

```java
public abstract class ResourceNotFoundException extends RuntimeException {
    protected ResourceNotFoundException(String message) { super(message); }
}
public class ProductNotFoundException extends ResourceNotFoundException { ... }
public class CustomerNotFoundException extends ResourceNotFoundException { ... }

// Um único handler passaria a cobrir todas (polimorfismo de exceção):
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<StandardError> handleNotFound(ResourceNotFoundException ex, ...) { ... }
```

Como cada serviço é um projeto Maven separado, o ideal seria colocar essa base numa **biblioteca compartilhada** (módulo comum).

**2. `StandardError` copiado em 4 serviços.** Mesma classe repetida. Também iria para a biblioteca compartilhada.

**3. Getters duplicados em `Product`.** A entidade tem `getStockQuantity()` **e** `getStock_quantity()` (e o mesmo para `createdAt`). Dois métodos para o mesmo dado confundem quem usa e violam o encapsulamento limpo. Mantenha apenas um, no padrão `camelCase`.

**4. Handlers repetitivos.** Cada handler monta um `StandardError` quase igual. Um método auxiliar privado (como `conflict(...)` no `OrderExceptionHandler`) elimina a repetição.

---

## 14. Perguntas de entrevista

**Quais são os 4 pilares da POO? Dê um exemplo de cada.**
Encapsulamento, abstração, herança e polimorfismo (tabela da seção 1).

**Qual a diferença entre classe abstrata e interface?**
Interface é um contrato (várias por classe, sem estado). Classe abstrata é uma base parcial com código e estado (só uma por classe).

**Quando usar herança e quando composição?**
Herança para "é um" genuíno e estável; composição ("tem um") para reaproveitar comportamento. Na dúvida, composição.

**O que é polimorfismo? Dê um exemplo prático.**
Tratar objetos de tipos diferentes pela mesma interface. Ex.: o `AuthService` usa `PasswordEncoder`, que em produção é BCrypt e no teste é um mock.

**Qual a diferença entre sobrecarga e sobrescrita?**
Sobrecarga: mesmo nome, parâmetros diferentes, mesma classe, resolvida na compilação. Sobrescrita: redefinir método herdado, resolvida em execução.

**Para que serve `@Override`?**
Faz o compilador conferir que o método realmente sobrescreve algo, evitando erros silenciosos.

**Por que em Java não há herança múltipla de classes?**
Para evitar o "problema do diamante" (ambiguidade quando duas mães definem o mesmo método). Interfaces suprem a necessidade.

**O que é injeção de dependência e por que ajuda?**
Receber as dependências de fora (construtor) em vez de criá-las. Reduz acoplamento e permite trocar/simular implementações em testes.

**Por que preferir `record` para DTOs?**
Imutável, conciso, com `equals`/`hashCode`/`toString` gerados.

**O que significa `final` em campo, método e classe?**
Campo: não pode ser reatribuído. Método: não pode ser sobrescrito. Classe: não pode ser herdada.

---

## 15. Exercícios

1. **Refatore as exceções.** Crie `ResourceNotFoundException` e faça as `*NotFoundException` do `order-service` estendê-la. Troque os três handlers por um só. Os testes continuam passando?
2. **Ache o polimorfismo.** No `OrderServiceTest`, liste três objetos que são mocks de **interfaces ou classes** e explique por que o `OrderService` não percebe a troca.
3. **Crie uma interface.** Extraia de `AuthCookieService` uma interface `TokenCookieFactory` com `create(token)` e `clear()`. O `AuthController` deveria depender da interface. Escreva uma implementação alternativa (por exemplo, sem `Secure`).
4. **Template Method.** Escreva sua própria classe abstrata `BaseHandler` com um método `final handle()` que faz log + chama um método abstrato `process()`. Crie duas filhas.
5. **Composição vs. herança.** Imagine que alguém propõe `OrderService extends ProductService` "para reaproveitar código". Escreva em 3 linhas por que isso é ruim.
6. **Limpe o `Product`.** Remova os getters/setters duplicados (`getStock_quantity`, `getCreated_at`) e corrija o que quebrar.
