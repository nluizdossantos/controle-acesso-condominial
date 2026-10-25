# 📅 Escala de Trabalho — App Android

Aplicativo Android para montar e consultar a escala da equipe do condomínio
(porteiros, zeladores, vigilantes, limpeza…). Funciona 100% offline, com os dados
salvos no próprio aparelho.

## Funcionalidades

- **Hoje** — quem está em serviço agora (inclusive turnos noturnos que começaram na véspera),
  demais turnos do dia e a escala de amanhã.
- **Calendário** — visão mensal com a cor de cada funcionário nos dias de trabalho,
  detalhes do dia selecionado e total de horas previstas por pessoa no mês.
- **Compartilhar** — envia a escala do mês em texto (WhatsApp, e-mail etc.).
- **Equipe** — cadastro de funcionários com cargo, telefone, cor e status ativo/inativo.
- **Escalas** — 12x36, 24x48, 6x1, 5x2, 4x2 ou personalizada (N dias de trabalho × M de folga),
  com horário de entrada/saída, posto e período de vigência.
- **Férias, atestados, folgas e trocas** — o turno aparece riscado no dia e sai das horas do mês.

## Como obter o APK

Cada push que altera `android-escala/` dispara o workflow
**App Android - Escala de Trabalho** no GitHub Actions. Ao final, baixe o artefato
`escala-de-trabalho-apk` na página da execução e instale no celular
(é preciso permitir "instalar apps de fontes desconhecidas").

> O APK de release é assinado com a chave de debug para facilitar a instalação direta.
> Para publicar na Play Store, configure uma chave própria em `app/build.gradle.kts`.

## Compilar localmente

Pré-requisitos: JDK 17+ e Android SDK (API 35) — o Android Studio já instala ambos.

```bash
cd android-escala
./gradlew testDebugUnitTest   # testes da lógica de escala
./gradlew assembleDebug       # APK em app/build/outputs/apk/debug/
```

Ou abra a pasta `android-escala` no Android Studio e clique em **Run**.

## Estrutura

```
app/src/main/java/br/com/condominio/escala/
├── domain/Escala.kt        # regras de escala e cálculos (Kotlin puro, testado)
├── data/                   # Room: funcionários, escalas e ausências
├── ui/                     # ViewModel, componentes e formatação pt-BR
│   └── screens/            # Hoje, Calendário, Equipe, Funcionário
└── MainActivity.kt         # navegação inferior
```

Tecnologias: Kotlin, Jetpack Compose (Material 3), Room, Navigation Compose. Android 8.0+ (API 26).
