# Trabalhos futuros

## Pausa segura da corrida

Uma implementacao futura de pausa deve tratar a pausa como estado sincronizado da sessao, nao apenas como botao visual. Ela deve pausar o cronometro efetivo da corrida, suspender a movimentacao e a progressao do jogo, evitar acumulo indevido de distancia ou telemetria, manter a sessao recuperavel e permitir retomada segura.

O tempo pausado deve ficar fora dos calculos de desempenho. Mobile e backend tambem precisam sincronizar esse estado, tratar desconexoes durante a pausa e garantir que operacoes de pausar e continuar sejam idempotentes. Os estados devem considerar conceitos como `ACTIVE`, `PAUSED`, `FINISHED` e `CANCELLED`, adaptados ao modelo existente do projeto.

## Altimetria

Uma evolucao futura pode adicionar corridas com ganho de altitude e percursos com perfis de elevacao diferentes. A solucao deve considerar altitude acumulada como metrica adicional, comparacao justa entre percursos com altimetrias diferentes e tratamento de ruido dos sensores de altitude.

No backend, essa evolucao pode armazenar distancia, ganho acumulado e perfil do percurso para permitir historico, comparacao e analises mais consistentes.
