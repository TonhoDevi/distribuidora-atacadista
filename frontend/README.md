# frontend

SPA em Angular — parte do sistema **Distribuidora Atacadista** (AtlasTT).

Responsável por: interface web para login e CRUD de clientes, produtos e pedidos, além de gestão de usuários (ADMIN). Consome exclusivamente o `gateway-service` (porta 8080) — nunca fala direto com os microsserviços de backend.

---

## Stack

- Angular 22 (standalone components, signals, `@if`/`@for` no template)
- Angular Material 22 (Material 3 / `mat.theme()`)
- Reactive Forms
- TypeScript, Vitest (test runner do Angular CLI)

---

## Pré-requisitos

- Node.js `^22.22.3` ou `^24.15.0` ou `>=26.0.0` (exigido pelo Angular CLI 22 — Node 22.22.2 ou anteriores **não funcionam**)
- `gateway-service` rodando em `http://localhost:8080`, com `eureka-server`, `auth-service`, `customer-service`, `product-service` e `order-service` registrados (ver READMEs de cada serviço)

---

## Rodando a aplicação

```bash
npm install
ng serve
```

A aplicação sobe em `http://localhost:4200`.

Login inicial: usuário `admin` e a senha definida em `ADMIN_DEFAULT_PASSWORD` no `auth-service` (ver `auth-service/README.md`, `AdminSeeder`).

---

## Telas

| Rota | Descrição | Acesso |
|---|---|---|
| `/login` | Autenticação (usuário/senha → JWT) | Pública |
| `/` | Dashboard com atalhos para os módulos | Autenticado |
| `/customers` | Listagem, busca por nome, criação, edição e exclusão de clientes | Autenticado |
| `/products` | Listagem, busca por nome, criação, edição e exclusão de produtos | Autenticado |
| `/orders` | Listagem, criação e detalhe de pedidos | Autenticado |
| `/users` | Listagem, criação, edição e exclusão de usuários do sistema | Apenas `ADMIN` |

---

## Estrutura do projeto

```
frontend/src/app/
├── core/
│   ├── guards/          → authGuard, roleGuard (proteção de rotas)
│   ├── interceptors/     → authInterceptor (injeta Bearer token), errorInterceptor (401 → logout)
│   └── models/           → StandardError (espelha o formato de erro do backend)
├── shared/
│   ├── components/       → ConfirmDialogComponent (confirmação de exclusão)
│   └── services/         → NotificationService (wrapper de MatSnackBar)
├── layout/                → LayoutComponent (shell com sidenav + navegação por role)
└── features/
    ├── auth/              → login, AuthService (token/username/role no localStorage)
    ├── home/               → dashboard
    ├── customers/          → model, service, list, form
    ├── products/           → model, service, list, form
    ├── orders/             → model, service, list, form (itens dinâmicos via FormArray), detail
    └── users/              → model, service, list, form (rota protegida por roleGuard(['ADMIN']))
```

Cada feature segue o mesmo padrão: `*.model.ts` (contratos alinhados aos DTOs Java dos serviços), `*.service.ts` (chamadas HTTP ao Gateway), rotas lazy-loaded via `loadComponent`/`loadChildren`, componentes standalone.

---

## Decisões de design

- **Tudo aponta pro Gateway (`http://localhost:8080`), nunca direto pro microsserviço** — mesmo princípio de ponto único de entrada documentado em `gateway-service/README.md`.
- **Autorização é só UX aqui.** `roleGuard` e a filtragem de itens de menu/dashboard por role só escondem telas — a autorização de verdade é aplicada no Gateway (`JwtGlobalFilter`), como já documentado nos guards existentes. Uma role indevida tentando `DELETE`/`POST /users` direto na API recebe 403 do Gateway independente do que o frontend mostra.
- **`auth-service`/`UserController` não expõe `GET /users/{id}`.** A tela de edição de usuário busca a lista inteira (`GET /users`) e filtra pelo id no cliente — não é uma limitação do frontend, é o contrato do backend.
- **Total do pedido é só uma prévia no formulário de criação.** O cálculo exibido em `/orders/new` (preço do produto × quantidade) é client-side, para UX; o valor gravado de fato vem sempre do `order-service`, que busca o preço vigente no `product-service` no momento da criação (ver `order-service/README.md`).
- **Sem endpoint de atualização/exclusão de pedido no backend.** `OrderController` só expõe `POST` e `GET` — por isso não há tela de edição/exclusão de pedido no frontend, só criação e consulta.
- **Senha é obrigatória mesmo editando um usuário**, porque `UserRequestDTO` é a mesma DTO usada por criação e atualização no `auth-service`, com `@NotBlank` em `password` — o formulário reflete essa exigência em vez de simular um campo opcional que a API rejeitaria.
- **Locale `pt-BR` registrado globalmente** (`registerLocaleData`, `LOCALE_ID`) para datas e valores em Real (`| currency:'BRL'`) formatados corretamente.

---

## Rodando os testes

```bash
ng test
```

## Build de produção

```bash
ng build
```

Artefatos em `dist/frontend/`.

---

## Status (Fase 6 do plano de aprendizado) — ✅ CONCLUÍDA

- [x] Projeto Angular standalone gerado, com Angular Material configurado
- [x] `AuthService` + `authInterceptor` (Bearer token) + `errorInterceptor` (401 → logout)
- [x] `authGuard` e `roleGuard` (proteção de rotas por autenticação/role)
- [x] Tela de login integrada ao `auth-service` via Gateway
- [x] Layout com sidenav e navegação filtrada por role
- [x] CRUD completo de Clientes (listar, buscar, criar, editar, excluir)
- [x] CRUD completo de Produtos (listar, buscar, criar, editar, excluir)
- [x] Pedidos: listagem, criação (seleção de cliente + itens dinâmicos) e detalhe
- [x] CRUD de Usuários restrito a `ADMIN` (listar, criar, editar, excluir)
- [x] Tratamento de erros centralizado via `NotificationService` (snackbars) usando o `StandardError` do backend
- [x] Build de produção validado dentro do orçamento de bundle do `angular.json`

O projeto segue com o desenvolvimento concentrado no backend — este frontend cobre a superfície necessária para operar o sistema (login + CRUDs), sem novas fases de UI planejadas por ora.
