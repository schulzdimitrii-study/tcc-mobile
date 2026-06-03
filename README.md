# TCC Mobile

Aplicativo mobile do projeto de TCC, desenvolvido em Kotlin Multiplatform com Compose Multiplatform. A aplicação combina autenticação, integração com backend, coleta de telemetria do dispositivo e uma experiência gamificada de corrida/fuga renderizada com KorGE.

O Android é a plataforma funcional principal. O iOS existe como wrapper multiplataforma, mas ainda não possui o mesmo nível de integração com telemetria e jogo visual.

## Visão Geral

O app permite que o usuário:

- crie conta e faça login no backend;
- carregue o catálogo de Hordas disponíveis;
- escolha uma Horda remota ou configure uma Horda local manualmente;
- inicie uma sessão de corrida;
- colete localização e aceleração no Android;
- transforme os sinais coletados em métricas de gameplay;
- envie dados biométricos e estado do jogo para o backend em tempo real;
- acompanhe progresso, risco, ranking e estado da sessão.

O fluxo principal é:

```text
Sensores Android -> DefaultTelemetryRepository -> TelemetryState
TelemetryState -> GameController -> GameSnapshot -> KorGE
TelemetryState + GameSnapshot -> WebSocket/STOMP -> Backend
```

## Tecnologias

- Kotlin Multiplatform 2.1.0
- Compose Multiplatform
- Material 3
- KorGE 5.4.0
- Android SDK 36
- Play Services Location
- Ktor Client
- OkHttp WebSocket
- Kotlinx Serialization
- Android DataStore
- Gradle Kotlin DSL

## Estrutura do Projeto

```text
.
├── composeApp
│   ├── src/commonMain
│   │   ├── kotlin/com/pedroaba/tccmobile
│   │   │   ├── features        # telas Compose
│   │   │   ├── game            # domínio, lógica e cena do jogo
│   │   │   ├── backend         # modelos compartilhados da integração
│   │   │   ├── ui              # componentes e navegação
│   │   │   └── theme           # tema visual
│   │   └── composeResources    # sprites e backgrounds do jogo
│   ├── src/androidMain
│   │   ├── kotlin/com/pedroaba/tccmobile
│   │   │   ├── auth            # login, cadastro e persistência de sessão
│   │   │   ├── backend         # HTTP, REST e WebSocket/STOMP
│   │   │   └── telemetry       # GPS, acelerômetro e foreground service
│   │   └── AndroidManifest.xml
│   ├── src/iosMain             # entrypoint iOS e placeholders
│   └── src/commonTest          # testes do domínio compartilhado
└── iosApp                      # shell SwiftUI para iOS
```

## Arquitetura

### Camada de Plataforma

No Android, `MainActivity` é o ponto de entrada funcional. Ela inicializa:

- `AuthManager`, responsável por login, cadastro e restauração de sessão;
- `AndroidTelemetryRuntime`, que agrupa serviços de localização, movimento e wearable;
- `OnlineSessionRepository`, responsável pela sessão remota, leaderboard e WebSocket;
- fluxo de permissões de localização e notificação;
- `TelemetryForegroundService`, que mantém a coleta ativa durante a corrida.

No iOS, `MainViewController()` apenas monta a UI Compose dentro do wrapper SwiftUI. A coleta de telemetria e a ponte KorGE ainda não têm implementação equivalente à do Android.

### Camada de Interface

A interface compartilhada fica em `commonMain` e é organizada por features:

- `features/auth`: login, cadastro e tela de carregamento;
- `features/home`: tela inicial, seleção de Horda e início da corrida;
- `features/game`: tela da sessão, status, telemetria e canvas KorGE;
- `features/ranking`: leaderboard da sessão;
- `features/profile`: perfil, edição de dados e permissões;
- `features/history` e `features/watch`: telas auxiliares.

O estado de navegação principal do app Android é controlado em `MainActivity`, porque a tela precisa coordenar autenticação, permissões, sessão online e telemetria nativa.

### Domínio do Jogo

O jogo é desacoplado dos sensores Android. Ele trabalha com métricas abstraídas, não com tipos nativos como `Location` ou `SensorEvent`.

Componentes principais:

- `GameController`: controla ciclo da sessão e expõe `GameSnapshot`;
- `RunCalculator`: calcula velocidade, distância, pressão da Horda, risco e resultado;
- `GameSnapshot`: contrato entre o domínio e a cena visual;
- `MainScene`: cena KorGE que renderiza corredor, Horda e parallax;
- `DistanceVisualMapper`: converte distância lógica em posição visual.

Na tela de jogo, `GameScreen` observa `TelemetryState.latestEscapeMetrics` e aplica essas métricas ao `GameController`. O resultado é refletido no `GameSnapshot`, que alimenta os componentes de status e a cena KorGE.

### Pipeline de Telemetria

A telemetria Android é centralizada em `DefaultTelemetryRepository`.

Entradas:

- `FusedLocationTrackingService`: localização via Play Services Location;
- `AndroidMotionSensorService`: aceleração do dispositivo;
- `WearTelemetryBridge`: contrato para wearable;
- `NoOpWearTelemetryBridge`: implementação atual sem smartwatch real.

Processamento compartilhado:

- `MovementTelemetryProcessor`: suaviza velocidade/aceleração, calcula distância e detecta dados obsoletos;
- `SelectTelemetryStrategyUseCase`: decide entre `MOVEMENT_ONLY` e `BPM_AND_MOVEMENT`;
- `ComputeEscapeMetricsUseCase`: converte amostras em métricas normalizadas para o jogo.

Saída:

- `TelemetryState.latestSample`;
- `TelemetryState.latestEscapeMetrics`;
- disponibilidade de sensores e permissões;
- estado da sessão de movimento.

## Integração com Backend

A integração Android usa HTTP REST e WebSocket/STOMP.

REST:

- `POST /auth/login`
- `POST /auth/register`
- `GET /sessions/hordes`
- `POST /sessions/start`
- `GET /sessions/{sessionId}/leaderboard`
- `POST /sessions/{sessionId}/finish`
- endpoints de usuário via `UserApi`

WebSocket:

- conexão em `/ws`;
- envio de telemetria para `/app/train/data`;
- inscrição em `/topic/session/{sessionId}/leaderboard`;
- inscrição em `/topic/session/{sessionId}/game-state`.

`OnlineSessionRepository` inicia a sessão no backend, conecta o WebSocket, recebe atualizações de ranking e envia, no máximo uma vez por segundo, uma mensagem combinando `TelemetryState` e `GameSnapshot`.

A URL padrão do backend está em:

```kotlin
composeApp/src/androidMain/kotlin/com/pedroaba/tccmobile/backend/BackendConfig.kt
```

## Autenticação

O Android usa `AuthManager` para autenticar no backend. Após login ou cadastro, o token JWT e dados básicos do usuário são persistidos com Android DataStore por `SessionManager`.

Quando o backend retorna falha de autenticação, `MainActivity` encerra a sessão local e retorna o usuário ao fluxo não autenticado.

## Permissões Android

Para iniciar uma corrida, o app verifica:

- permissão de localização fina/aproximada;
- permissão de notificação em Android 13+;
- provedor de localização ativo;
- disponibilidade de sensor de movimento.

Se algum requisito estiver ausente, a navegação direciona o usuário para a tela de permissões de telemetria.

## iOS

O projeto compila targets iOS `iosArm64` e `iosSimulatorArm64`, com framework estático `ComposeApp`.

Limitações atuais:

- sem runtime nativo de telemetria;
- sem coleta de sensores equivalente ao Android;
- `KorgeGameView.ios.kt` ainda é placeholder;
- o wrapper SwiftUI hospeda apenas o `MainViewController()` compartilhado.

## Testes

O projeto possui testes em `commonTest`, `androidUnitTest` e `androidInstrumentedTest`.

Áreas com cobertura relevante:

- `GameController`;
- `RunCalculator`;
- `MovementTelemetryProcessor`;
- `ComputeEscapeMetricsUseCase`;
- `RemoteSessionMapper`;
- decodificação de leaderboard;
- componentes instrumentados de UI.

## Comandos Úteis

Recomendação de ambiente: use JDK 17 ou 21 para rodar o Gradle. Com OpenJDK 25, o build pode falhar durante a avaliação dos scripts Kotlin com erro `IllegalArgumentException: 25.0.2`.

Compilar o app Android:

```bash
./gradlew :composeApp:assembleDebug
```

Compilar Kotlin Android:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Executar testes compartilhados:

```bash
./gradlew :composeApp:allTests
```

Compilar iOS simulator:

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

## Observações para Desenvolvimento

- Preserve o fluxo `TelemetryState -> GameController -> GameSnapshot -> KorGE`.
- Não acople o domínio compartilhado a tipos Android.
- Novas fontes de sinal devem entrar pelo pipeline de telemetria, não diretamente pela UI.
- Regras de gameplay devem ficar em `game/*`, especialmente em `RunCalculator` e `GameController`.
- A referência funcional atual é Android; iOS ainda não tem paridade.
