# =====================================================================
# gestao-verbas — demonstracao automatizada (PowerShell 5.1 e 7)
# Nao apaga dados existentes. Nao acessa nada alem da API informada em -BaseUrl.
# Uso:
#   powershell -ExecutionPolicy Bypass -File scripts\demo.ps1
#   pwsh -File scripts/demo.ps1
#   .\scripts\demo.ps1 -BaseUrl http://localhost:8080
# =====================================================================
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Escrever-Passo([string]$Mensagem) {
    Write-Host "" $Mensagem "" -ForegroundColor Cyan
}

function Escrever-Ok([string]$Mensagem) {
    Write-Host "  [OK] $Mensagem" -ForegroundColor Green
}

function Escrever-Erro([string]$Mensagem) {
    Write-Host "  [ERRO] $Mensagem" -ForegroundColor Red
}

try {
    Escrever-Passo "Demonstracao do gestao-verbas — API: $BaseUrl"
    Escrever-Passo "Cenario: departamento com orcamento 10000, duas solicitacoes de 7000;"
    Escrever-Passo "a primeira e aprovada (reserva 7000) e a segunda deve falhar com HTTP 409."

    # ------------------------------------------------------------------
    # 1) Criar departamento com orcamento 10000
    Escrever-Passo "PASSO 1: criando departamento com orcamento 10000.00"
    $deptCorpo = @{ nome = "Demo Departamento $(Get-Date -Format 'yyyyMMddHHmmss')"; orcamentoTotal = 10000.00 } | ConvertTo-Json
    $dept = Invoke-RestMethod -Method Post -Uri "$BaseUrl/departamentos" -ContentType 'application/json; charset=utf-8' -Body $deptCorpo
    $deptId = $dept.id
    Escrever-Ok "Departamento criado: id=$deptId, saldoDisponivel=$($dept.saldoDisponivel)"

    # ------------------------------------------------------------------
    # 2) Abrir duas solicitacoes de 7000
    Escrever-Passo "PASSO 2: abrindo duas solicitacoes de 7000.00"
    $solCorpo = @{ descricao = 'Solicitacao A (demo)'; valor = 7000.00 } | ConvertTo-Json
    $solA = Invoke-RestMethod -Method Post -Uri "$BaseUrl/departamentos/$deptId/solicitacoes" -ContentType 'application/json; charset=utf-8' -Body $solCorpo
    $solCorpo = @{ descricao = 'Solicitacao B (demo)'; valor = 7000.00 } | ConvertTo-Json
    $solB = Invoke-RestMethod -Method Post -Uri "$BaseUrl/departamentos/$deptId/solicitacoes" -ContentType 'application/json; charset=utf-8' -Body $solCorpo
    Escrever-Ok "Solicitacao A: id=$($solA.id), status=$($solA.status)"
    Escrever-Ok "Solicitacao B: id=$($solB.id), status=$($solB.status)"

    # ------------------------------------------------------------------
    # 3) Aprovar a primeira solicitacao
    Escrever-Passo "PASSO 3: aprovando a solicitacao A (responsavel ficticio 'Maria Analista')"
    $aprovarCorpo = @{ responsavel = 'Maria Analista' } | ConvertTo-Json
    $aprovada = Invoke-RestMethod -Method Post -Uri "$BaseUrl/solicitacoes/$($solA.id)/aprovar" -ContentType 'application/json; charset=utf-8' -Body $aprovarCorpo
    Escrever-Ok "Solicitacao A aprovada: status=$($aprovada.status)"

    # ------------------------------------------------------------------
    # 4) Consultar saldo atual e historico
    Escrever-Passo "PASSO 4: consultando saldo do departamento e historico da solicitacao A"
    $deptAtual = Invoke-RestMethod -Method Get -Uri "$BaseUrl/departamentos/$deptId"
    Escrever-Ok "valorComprometido=$($deptAtual.valorComprometido), saldoDisponivel=$($deptAtual.saldoDisponivel) (esperado: 7000 / 3000)"
    $historico = Invoke-RestMethod -Method Get -Uri "$BaseUrl/solicitacoes/$($solA.id)/historico"
    $historico | ConvertTo-Json -Depth 5 | Write-Host

    # ------------------------------------------------------------------
    # 5) Tentar aprovar a segunda solicitacao — deve falhar com HTTP 409
    Escrever-Passo "PASSO 5: tentando aprovar a solicitacao B — esperado HTTP 409 (saldo insuficiente)"
    $codigoStatus = 0
    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/solicitacoes/$($solB.id)/aprovar" -ContentType 'application/json; charset=utf-8' -Body $aprovarCorpo | Out-Null
        # Se chegou aqui, a aprovacao inesperadamente ocorreu — erro grave.
        Escrever-Erro "A aprovacao da solicitacao B ocorreu, mas deveria ter falhado com HTTP 409 (saldo insuficiente: 3000 < 7000)."
        throw "FALHA DA DEMO: regra de saldo insuficiente nao foi respeitada pela API."
    }
    catch {
        $resp = if ($_.Exception.PSObject.Properties['Response']) { $_.Exception.Response } else { $null }
        if ($null -ne $resp) {
            if ($resp -is [System.Net.HttpWebResponse]) {
                $codigoStatus = [int]$resp.StatusCode
            }
            else {
                # PowerShell 7 (HttpClient): StatusCode via propriedade numerica
                $codigoStatus = [int]$resp.StatusCode
            }
        }
        if ($codigoStatus -eq 409) {
            Escrever-Ok "Recebido HTTP 409 conforme esperado: saldo insuficiente bloqueou a aprovacao."
        }
        else {
            Escrever-Erro "Erro inesperado (HTTP $codigoStatus). Esperava-se 409. Detalhe: $($_.Exception.Message)"
            throw "FALHA DA DEMO: esperava HTTP 409, obtive HTTP $codigoStatus."
        }
    }

    # ------------------------------------------------------------------
    # 6) Estado final
    Escrever-Passo "PASSO 6: estado final"
    $deptFinal = Invoke-RestMethod -Method Get -Uri "$BaseUrl/departamentos/$deptId"
    $solBFinal = Invoke-RestMethod -Method Get -Uri "$BaseUrl/solicitacoes/$($solB.id)"
    if ($deptFinal.valorComprometido -ne 7000 -or $deptFinal.saldoDisponivel -ne 3000 -or $solBFinal.status -ne 'PENDENTE' -or @($historico).Count -ne 1) {
        throw 'FALHA DA DEMO: saldo, situacao ou historico final divergente.'
    }
    Escrever-Ok "Departamento ${deptId}: comprometido=$($deptFinal.valorComprometido), saldo=$($deptFinal.saldoDisponivel)"
    Escrever-Ok "Solicitacao B permanece: status=$($solBFinal.status) (esperado: PENDENTE)"

    Write-Host ""
    Write-Host "Demonstracao concluida com sucesso: aprovacao reserva saldo e segunda aprovacao foi rejeitada com HTTP 409." -ForegroundColor Green
    exit 0
}
catch {
    Escrever-Erro "Demonstracao falhou: $($_.Exception.Message)"
    Write-Host "Verifique se a API esta ativa em $BaseUrl (docker-compose -f compose.yaml --profile app up --build -d no Ubuntu WSL)." -ForegroundColor Yellow
    exit 1
}
