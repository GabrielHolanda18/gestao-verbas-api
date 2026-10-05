# Executar no Ubuntu WSL2

O Docker funcional desta máquina está no **Ubuntu WSL2**. O launcher Docker Desktop do Windows não é necessário. Foram validados Docker Engine 29.1.3 e `docker-compose` 1.29.2. Em instalações com Compose v2, substitua `docker-compose` por `docker compose`, mantendo `-f compose.yaml`.

## Abrir o projeto

No PowerShell do Windows:

```powershell
wsl -d Ubuntu
```

**Mantenha o terminal Ubuntu aberto enquanto usar a API.** Nesta máquina, encerrar a sessão do WSL também encerrou os containers.

A partir daqui, no terminal Ubuntu:

```bash
cd '/mnt/c/Users/gabri/OneDrive/Área de Trabalho/Projeto Java/gestao-verbas'
docker info
```

Na cópia local, `.env` já está configurado com uma senha aleatória para o banco. Não o sobrescreva enquanto utilizar o volume atual. Em um clone novo, execute `cp .env.example .env` e edite `POSTGRES_PASSWORD`. O arquivo `.env` é ignorado pelo Git.

## Subir banco e API

```bash
docker-compose -f compose.yaml --profile app up --build -d
docker-compose -f compose.yaml ps
docker-compose -f compose.yaml logs --tail=60 app
```

API em `http://localhost:8080`; PostgreSQL publicado apenas em `127.0.0.1:5433`. A primeira compilação baixa as dependências. Se a porta 8080 já estiver ocupada, pare o serviço conflitante ou ajuste a porta de publicação e a URL da demonstração.

## Executar todos os testes

```bash
sh scripts/testar-wsl.sh
```

O script usa um container Maven com Java 21, sem instalar Java no Ubuntu. O Maven Wrapper fixa Maven 3.9.16. O Testcontainers cria PostgreSQL temporários e remove-os no fim. A rede do host permite que os testes alcancem as portas temporárias no WSL. O socket Docker é montado somente no container de testes; a aplicação não recebe esse acesso.

O cache `gestao-verbas-maven-cache` evita baixar novamente as dependências. Não é o volume de dados do aplicativo. Se Docker não estiver disponível, a execução falha; testes de integração não são ignorados silenciosamente.

## Demonstração

Com a API pronta, no PowerShell do Windows, dentro da pasta `gestao-verbas`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo.ps1
```

A demonstração cria registros próprios, aprova um pedido, confere saldo/histórico e exige HTTP 409 para o pedido sem saldo. No Ubuntu, `curl http://localhost:8080/solicitacoes` consulta a API. Não existe frontend ou página inicial neste escopo.

## Parar e reiniciar preservando os dados

No Ubuntu:

```bash
docker-compose -f compose.yaml restart app
docker-compose -f compose.yaml --profile app down
# Para subir novamente:
docker-compose -f compose.yaml --profile app up -d
```

O comando `down` acima preserva o volume nomeado. Uma nova senha no `.env` não redefine a senha de um PostgreSQL cujo volume já existe. Preserve sua configuração.

## Java local no Windows (opcional)

Existe um JDK 21 em `C:\Users\gabri\.jdks\corretto-21.0.10`. O Java padrão do PATH era Java 8; configurar apenas o JAVA_HOME permite que o Maven Wrapper use o JDK correto:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\corretto-21.0.10"
.\mvnw.cmd test
```

Para iniciar a aplicação fora do container, mantenha apenas o banco no WSL, configure `DB_PASSWORD` com o mesmo valor de `POSTGRES_PASSWORD` do `.env` e execute `.\mvnw.cmd spring-boot:run`. Spring não lê `.env` automaticamente. Para integração use o script no WSL, onde o Docker está disponível.
