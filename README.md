 <picture>
    <img src="https://i.imgur.com/cQruGU2.png" alt="GEST Logo">
  </picture>

---
<p align="center">
  <a href="#introdução">Introdução</a> —
  <a href="#requisitos">Requisitos</a> —
  <a href="#gestão-do-projeto">Gestão do Projeto</a> —
  <a href="#análise-e-projeto-do-software">Análise e Projeto do Software</a> —
  <a href="#conclusão">Conclusão</a>
</p>


---

## Introdução

O GEST (Gestão de core.Estoque) é um sistema desenvolvido com o objetivo de auxiliar no controle e gerenciamento de produtos em estoque de uma pequena empresa. O sistema permite o cadastro, consulta, atualização e remoção de produtos, bem como o registro de entradas e saídas, fornecendo relatórios que auxiliam na tomada de decisões relacionadas ao estoque, como produtos em falta, valor total armazenado e lucro estimado.

Inicialmente, o GEST foi concebido como um projeto acadêmico na disciplina de Programação Orientada a Objetos (POO), onde seu foco principal foi a modelagem do domínio do problema e a aplicação dos conceitos fundamentais de orientação a objetos, como encapsulamento, herança, polimorfismo e abstração.

<p align="center">
  <img src="https://i.imgur.com/NyAl882.png"/>
  <br>
  <em>Figura 1 – Versão POO</em>
</p>


Na disciplina de Processo de Desenvolvimento de Software (PDS), o sistema passa por um processo de expansão e aprimoramento, deixando de ser apenas uma aplicação conceitual para se tornar um sistema mais completo e próximo de um cenário real. O objetivo nesta nova etapa é desenvolver uma interface gráfica intuitiva para usuários finais, integrar o sistema a um banco de dados, além de aplicar práticas de engenharia de software, como metodologias ágeis, planejamento incremental e boas práticas de projeto.

<p align="center">
  <img src="https://i.imgur.com/dAKdR0y.png"/>
  <br>
  <em>Figura 2 – Versão PDS (BETA)</em> 
</p>

---

## Requisitos

Esta seção apresenta os requisitos funcionais e não-funcionais do sistema.

### Histórias do Projeto

**História 1 — Configuração do banco de dados (PostgreSQL)**  
*Como desenvolvedor, quero fazer a conexão com o banco de dados PostgreSQL, para garantir que os dados do sistema sejam salvos de forma persistente e segura.*  
**Status:** ✅ 

**História 2 — Criar tela inicial de navegação**  
*Como usuário, quero uma tela inicial gráfica com menus de navegação claros, para acessar facilmente as funcionalidades de estoque, vendas e relatórios sem fechar a aplicação.*  
**Status:** ✅

**História 3 — Criar tela de login e autenticação**  
*Como administrador, quero uma tela de autenticação para o sistema, para impedir que usuários não autorizados acessem ou modifiquem dados sensíveis.*  
**Status:** ✅

**História 4 — Criar tela para o cadastro de fornecedores**  
*Como gerente, quero um formulário gráfico para cadastrar fornecedores, para manter um registro organizado.*  
**Status:** 🕒 

**História 5 — Criar tela para o cadastro de produtos**  
*Como estoquista, quero uma tela para cadastrar novos produtos, para inserir novos itens no catálogo do sistema.*  
**Status:** 🕒 

**História 6 — Criar tela de movimentação de saída de estoque**  
*Como vendedor, quero uma tela para buscar um produto e lançar saídas, para atualizar o saldo do estoque em tempo real conforme as vendas acontecem.*  
**Status:** 🕒 

**História 7 — Criar listagem de contatos de fornecedores**  
*Como gerente, quero ver uma lista com todos os fornecedores e seus contatos, para facilitar a comunicação e consulta de dados quando necessário.*  
**Status:** 🕒 

**História 8 — Criar tabela de listagem de produtos**  
*Como estoquista, quero ver uma tabela com todos os produtos cadastrados, para conferir rapidamente o inventário disponível e suas características.*  
**Status:** 🕒 

**História 9 — Criar lançamento de itens em lote**  
*Como estoquista, quero lançar a entrada de múltiplos itens de uma só vez na mesma operação, para agilizar o processo de reposição.*  
**Status:** 🕒 

**História 10 — Criar campos dinâmicos por tipo de produto**  
*Como estoquista, quero uma tela que adapte os campos visíveis (ex: mostrar “Validade” apenas para perecíveis), para preencher apenas as informações relevantes para cada tipo de produto.*  
**Status:** 🕒 

**História 11 — Criar registro de logs de alterações**  
*Como administrador, quero que o sistema registre no banco quem realizou cada alteração importante, para ter rastreabilidade e controle sobre as ações dos usuários.*  
**Status:** 🕒 

**História 12 — Criar alertas visuais de situação crítica dos produtos**  
*Como estoquista, quero visualizar um destaque visual para produtos em situação crítica (estoque baixo ou vencimento próximo), para priorizar a reposição ou promoção desses itens.*  
**Status:** 🕒 

**História 13 — Criar dashboard com indicadores**  
*Como dono da loja, quero uma tela inicial com indicadores visuais sobre a saúde do negócio, para obter uma visão rápida dos principais dados do sistema.*  
**Status:** 🕒 

**História 14 — Exportação de dados para arquivo**  
*Como gerente, quero exportar a lista de produtos para um arquivo externo (ex: .csv), para realizar conferências físicas ou envio de relatórios.*  
**Status:** 🕒

### Requisitos Não-Funcionais

**RNF-01 — Plataforma**  
O sistema deverá ser uma aplicação **desktop**, desenvolvida em **Java**, utilizando **JavaFX** para a interface gráfica.

**RNF-02 — Banco de dados**  
O sistema deverá utilizar **PostgreSQL** como sistema gerenciador de banco de dados, com conexão local via JDBC.

**RNF-03 — Usabilidade**  
A interface gráfica deverá ser **intuitiva e de fácil navegação**.

**RNF-04 — Desempenho**  
As operações de consulta, cadastro e atualização deverão responder em um tempo **inferior a 2 segundos** em ambiente local.

**RNF-05 — Segurança**  
O acesso ao sistema deverá ser protegido por autenticação de usuários, garantindo que apenas usuários autorizados possam acessar certas funcionalidades e informações.

**RNF-06 — Integridade dos dados**  
O sistema deverá garantir a integridade dos dados armazenados, evitando inconsistências em operações de entrada e saída de estoque.

**RNF-07 — Manutenibilidade**  
O código do sistema deverá ser organizado em camadas, facilitando manutenção, evolução e correções futuras.

**RNF-08 — Portabilidade**  
O sistema deverá ser compatível com os sistemas operacionais **Windows** e **Linux**.

---

## Gestão do Projeto

### Metodologia Utilizada

A metodologia utilizada no desenvolvimento do sistema foi o **Scrum**, conforme orientado na disciplina.  
A ferramenta Jira foi utilizada para organizar o backlog, as sprints e as tarefas do projeto.

### Papéis da Equipe

- **Product Owner (PO):** Alexander  
- **Desenvolvedor:** Felipe  

### Números do Projeto

- **Data de kick-off:** 19/12/2025  
- **Total de sprints planejadas:** 4
- **Sprint Atual:** 2 (Configuração do Banco de dados e UI)  
    - **Período da Sprint 1:** 19/12/2025 a 06/02/2026
    - **Período da Sprint 2:** 07/02/2026 a 20/02/2026   

### Transbordo de Tarefas

Não houve transbordo de tarefas entre sprints, pois o projeto encontra-se em fase inicial e ainda não entrou na etapa de desenvolvimento das funcionalidades.

### Backlog Inicial e Backlog Atual

O backlog inicial foi composto pelas principais histórias do sistema, relacionadas a:

- Configuração do banco de dados;
- Telas de cadastro e listagem;
- Movimentação de estoque;
- Dashboard e relatórios.

O backlog atual houve alterações, necessidades surgiram e viraram histórias.  
O sistema atualmente está com o dashboard e a tela de consulta de produtos funcioando.

---

## Análise e Projeto do Software

### Projeto Arquitetural

A arquitetura do sistema GEST segue o modelo de **arquitetura em camadas**, com separação entre interface gráfica, lógica de negócio e acesso a dados.

A estrutura geral do sistema é composta pelas seguintes camadas:

- **Camada de Apresentação (View):** responsável pela interface gráfica desenvolvida em JavaFX.
- **Camada de Controle (Controller):** responsável por receber as ações do usuário e coordenar as operações do sistema.
- **Camada de Serviço (Service):** responsável pelas regras de negócio.
- **Camada de Acesso a Dados (DAO):** responsável pela comunicação com o banco de dados PostgreSQL via JDBC.
- **Camada de Persistência:** banco de dados PostgreSQL.

Fluxo simplificado da arquitetura:

<p align="center">
  <img src="https://i.imgur.com/4lMjtEw.png"/>
  <br>
  <em>Figura 3 – Fluxo simplificado da arquitetura</em>
</p>

### Projeto de Componentes

O sistema será organizado em pacotes (módulos) conforme sua responsabilidade, seguindo uma estrutura semelhante a:

- **view:** telas JavaFX do sistema.
- **controller:** classes responsáveis por tratar eventos da interface.
- **model:** classes que representam as entidades do sistema (core.Produto, core.Fornecedor, Usuário, etc).
- **service:** regras de negócio.
- **dao:** classes responsáveis pelo acesso ao banco de dados.
- **util/config:** classes auxiliares, como conexão com o banco.

### Propriedades e Princípios de Projeto

O projeto do sistema segue os seguintes princípios e boas práticas:

- **Separação de responsabilidades:** cada classe possui uma função específica dentro do sistema.
- **Baixo acoplamento:** as camadas do sistema se comunicam através de interfaces bem definidas.
- **Alta coesão:** classes relacionadas possuem responsabilidades similares.
- **Padrão MVC (Model-View-Controller):** separação entre dados, interface e controle.
- **Padrão DAO (Data Access Object):** isolamento da lógica de acesso ao banco de dados.

---

## Conclusão

O projeto encontra-se em desenvolvimento (Sprint 2).
